package rw.auca.ac.pharmacy.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.PrescriptionCreateRequest;
import rw.auca.ac.pharmacy.dto.PrescriptionDto;
import rw.auca.ac.pharmacy.dto.PrescriptionFillRequest;
import rw.auca.ac.pharmacy.dto.PrescriptionItemDto;
import rw.auca.ac.pharmacy.dto.PrescriptionUpdateRequest;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages prescription lifecycle including creation, dispensing, and billing.
 * 
 * Real-world pharmacy flow:
 * 1. Doctor writes prescription (create)
 * 2. Patient brings prescription to pharmacy
 * 3. Pharmacist dispenses medicines (fill) - can be partial
 * 4. Bill is auto-generated/updated based on dispensed quantities
 * 5. Patient pays bill when prescription is fully dispensed
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrescriptionService {

    private static final String PRESCRIPTION_NOT_FOUND = "Prescription not found with ID: ";

    private final PrescriptionRepository prescriptionRepo;
    private final PersonRepository personRepo;
    private final PersonService personService;
    private final MedicineRepository medicineRepo;
    private final BillRepository billRepo;

    // ========== PRESCRIPTION CRUD ==========

    public PrescriptionDto create(PrescriptionCreateRequest request) {
        // Validate patient and pharmacist
        Person patient = request.getPatientId() != null
                ? personRepo.findById(request.getPatientId())
                        .orElseThrow(() -> new IllegalArgumentException("Patient not found"))
                : personService.getOrCreateAnonymousPatient();
        Long pharmacistId = currentUserPersonId();
        if (pharmacistId == null) {
            throw new IllegalArgumentException("Pharmacist not found");
        }
        Person pharmacist = personRepo.findById(pharmacistId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacist not found"));

        if (pharmacist.getRole() != Role.PHARMACIST && pharmacist.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only pharmacists can issue prescriptions");
        }

        Map<Long, Integer> requested = aggregateCreateQuantities(request.getItems());
        Map<Long, Medicine> medicines = loadMedicines(requested.keySet());
        validateSufficientStock(medicines, requested);
        applyStockDeltas(medicines, requested);
        medicineRepo.saveAll(new ArrayList<>(medicines.values()));

        // Create prescription
        Prescription rx = Prescription.builder()
                .dateIssued(request.getDateIssued() != null ? request.getDateIssued() : LocalDateTime.now())
                .status(PrescriptionStatus.PENDING)
                .person(patient)
                .pharmacist(pharmacist)
                .notes(request.getNotes())
                .walkInName(request.getWalkInName())
                .walkInPhone(request.getWalkInPhone())
                .items(new ArrayList<>())
                .build();
        rx = prescriptionRepo.save(rx);

        // Add items
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                Medicine med = medicines.get(itemReq.getMedicineId());
                if (med == null) {
                    throw new IllegalArgumentException("Medicine not found");
                }

                rx.getItems().add(PrescriptionItem.builder()
                        .prescription(rx)
                        .medicine(med)
                        .quantity(itemReq.getQuantity())
                        .dispensedQuantity(0)
                        .dispensed(false)
                        .dosage(itemReq.getDosage())
                        .frequency(itemReq.getFrequency())
                        .duration(itemReq.getDuration())
                        .instructions(itemReq.getInstructions())
                        .build());
            }
            rx = prescriptionRepo.save(rx);
        }

        log.info("Created prescription #{} for patient {}", rx.getId(), patient.getName());
        return toDto(rx);
    }

    public PrescriptionDto get(@NonNull Long id) {
        Prescription rx = findById(id);
        checkPatientAccess(rx);
        return toDto(rx);
    }

    public Page<PrescriptionDto> getAll(@NonNull Pageable pageable, String id, PrescriptionStatus status,
            String patientName, String pharmacistName, String q) {

        Specification<Prescription> spec = Specification.where(null);

        if (id != null && !id.isBlank()) {
            String idTerm = id.trim();
            spec = spec.and((r, cq, cb) -> cb.like(cb.toString(r.get("id")), idTerm + "%"));
        }

        if (isPatient()) {
            Long myId = currentUserPersonId();
            if (myId == null)
                return Page.empty(pageable);
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("person").get("id"), myId));
        } else if (isPharmacist() && !isAdmin()) {
            Long myId = currentUserPersonId();
            if (myId == null)
                return Page.empty(pageable);
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("pharmacist").get("id"), myId));
        }

        if (status != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("status"), status));
        }
        if (!isPatient() && patientName != null && !patientName.isBlank()) {
            spec = spec.and((r, cq, cb) -> cb.like(cb.lower(r.get("person").get("name")),
                    "%" + patientName.toLowerCase() + "%"));
        }
        if (pharmacistName != null && !pharmacistName.isBlank()) {
            spec = spec.and((r, cq, cb) -> cb.like(cb.lower(r.get("pharmacist").get("name")),
                    "%" + pharmacistName.toLowerCase() + "%"));
        }

        if (q != null && !q.isBlank()) {
            spec = spec.and(searchSpec(q.trim()));
        }

        return prescriptionRepo.findAll(spec, pageable).map(this::toDto);
    }

    public PrescriptionDto update(@NonNull Long id, PrescriptionUpdateRequest request) {
        Prescription rx = findById(id);

        if (rx.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a cancelled prescription");
        }

        if (request.getDateIssued() != null)
            rx.setDateIssued(request.getDateIssued());
        if (request.getNotes() != null)
            rx.setNotes(request.getNotes());
        if (request.getWalkInName() != null)
            rx.setWalkInName(request.getWalkInName());
        if (request.getWalkInPhone() != null)
            rx.setWalkInPhone(request.getWalkInPhone());

        // Status change validation - FILLED can ONLY be set by dispensing, not manually
        boolean cancelling = false;
        if (request.getStatus() != null) {
            PrescriptionStatus currentStatus = rx.getStatus();
            PrescriptionStatus newStatus = request.getStatus();

            // Can't change from FILLED to anything
            if (currentStatus == PrescriptionStatus.FILLED) {
                throw new IllegalStateException("Cannot change status of a filled prescription");
            }

            if (currentStatus == PrescriptionStatus.CANCELLED) {
                throw new IllegalStateException("Cannot change status of a cancelled prescription");
            }

            // Can't manually set to FILLED - must dispense
            if (newStatus == PrescriptionStatus.FILLED) {
                throw new IllegalArgumentException(
                        "Cannot manually set FILLED status. Dispense items to complete the prescription.");
            }

            if (newStatus == PrescriptionStatus.CANCELLED) {
                cancelling = true;
                Map<Long, Integer> remaining = aggregateRemainingQuantities(rx.getItems());
                if (!remaining.isEmpty()) {
                    Map<Long, Medicine> medicines = loadMedicines(remaining.keySet());
                    for (var entry : remaining.entrySet()) {
                        Medicine med = medicines.get(entry.getKey());
                        if (med != null) {
                            int available = safeQty(med.getQuantity());
                            med.setQuantity(available + entry.getValue());
                        }
                    }
                    medicineRepo.saveAll(new ArrayList<>(medicines.values()));
                    log.info("Restored {} item(s) to stock for cancelled prescription #{}", remaining.size(), id);
                }
            }

            // Only allow: PENDING → CANCELLED
            rx.setStatus(newStatus);
        }

        // Replace items if provided
        if (!cancelling && request.getItems() != null && !request.getItems().isEmpty()) {
            boolean hasDispensed = rx.getItems().stream()
                    .anyMatch(item -> item.getDispensedQuantity() != null && item.getDispensedQuantity() > 0);
            if (hasDispensed) {
                throw new IllegalStateException("Cannot edit items after dispensing has started");
            }
            if (rx.getStatus() == PrescriptionStatus.CANCELLED) {
                throw new IllegalStateException("Cannot edit items on a cancelled prescription");
            }

            Map<Long, Integer> current = aggregateExistingQuantities(rx.getItems());
            Map<Long, Integer> next = aggregateUpdateQuantities(request.getItems());
            Set<Long> allIds = new HashSet<>();
            allIds.addAll(current.keySet());
            allIds.addAll(next.keySet());

            Map<Long, Integer> deltas = new HashMap<>();
            for (Long medId : allIds) {
                int delta = next.getOrDefault(medId, 0) - current.getOrDefault(medId, 0);
                if (delta != 0) {
                    deltas.put(medId, delta);
                }
            }

            Map<Long, Medicine> medicines = loadMedicines(allIds);
            Map<Long, Integer> increases = new HashMap<>();
            for (var entry : deltas.entrySet()) {
                if (entry.getValue() > 0) {
                    increases.put(entry.getKey(), entry.getValue());
                }
            }
            validateSufficientStock(medicines, increases);
            applyStockDeltas(medicines, deltas);
            medicineRepo.saveAll(new ArrayList<>(medicines.values()));

            rx.getItems().clear();
            prescriptionRepo.saveAndFlush(rx);

            for (var itemReq : request.getItems()) {
                Medicine med = medicines.get(itemReq.getMedicineId());
                if (med == null) {
                    throw new IllegalArgumentException("Medicine not found");
                }

                rx.getItems().add(PrescriptionItem.builder()
                        .prescription(rx)
                        .medicine(med)
                        .quantity(itemReq.getQuantity())
                        .dosage(itemReq.getDosage())
                        .frequency(itemReq.getFrequency())
                        .duration(itemReq.getDuration())
                        .instructions(itemReq.getInstructions())
                        .build());
            }
        }

        return toDto(prescriptionRepo.save(rx));
    }

    public void delete(@NonNull Long id) {
        Prescription rx = findById(id);
        if (isPharmacist() && !isAdmin()) {
            Long myId = currentUserPersonId();
            if (myId == null || rx.getPharmacist() == null || !myId.equals(rx.getPharmacist().getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
        if (rx.getStatus() == PrescriptionStatus.FILLED) {
            throw new IllegalStateException("Cannot delete a filled prescription");
        }
        if (rx.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot delete a cancelled prescription");
        }
        Bill bill = rx.getBill();
        if (bill != null && bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Cannot delete a prescription with a paid bill");
        }
        boolean hasDispensed = rx.getItems().stream()
                .anyMatch(item -> item.getDispensedQuantity() != null && item.getDispensedQuantity() > 0);
        if (hasDispensed) {
            throw new IllegalStateException("Cannot delete a prescription after dispensing has started");
        }
        Map<Long, Integer> remaining = aggregateRemainingQuantities(rx.getItems());
        if (rx.getStatus() != PrescriptionStatus.CANCELLED && !remaining.isEmpty()) {
            Map<Long, Medicine> medicines = loadMedicines(remaining.keySet());
            for (var entry : remaining.entrySet()) {
                Medicine med = medicines.get(entry.getKey());
                if (med != null) {
                    int available = safeQty(med.getQuantity());
                    med.setQuantity(available + entry.getValue());
                }
            }
            medicineRepo.saveAll(new ArrayList<>(medicines.values()));
        }
        rx.setDeletedAt(LocalDateTime.now());
        prescriptionRepo.save(rx);
        log.warn("Soft-deleted prescription #{}", id);
    }

    // ========== DISPENSE (FILL) ==========

    /**
     * Dispense medicines for a prescription.
     * 
     * This is the core pharmacy workflow:
     * - Pharmacist selects quantities to dispense (can be partial)
     * - Stock is reduced by the dispensed amount
     * - Bill is auto-created/updated with current total
     * - Prescription becomes FILLED when all items fully dispensed
     */
    @Transactional
    public PrescriptionDto fill(@NonNull Long id, PrescriptionFillRequest request) {
        Prescription rx = findById(id);

        // Validate prescription is still pending
        if (rx.getStatus() == PrescriptionStatus.FILLED) {
            throw new IllegalStateException("Prescription is already fully dispensed");
        }
        if (rx.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot dispense a cancelled prescription");
        }

        // Block if bill is already paid (shouldn't dispense more after payment)
        Bill bill = rx.getBill();
        if (bill != null && bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Cannot dispense: bill has already been paid");
        }

        // Validate request
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Must specify items to dispense");
        }

        // Map items for quick lookup
        Map<Long, PrescriptionItem> itemMap = rx.getItems().stream()
                .collect(Collectors.toMap(PrescriptionItem::getId, Function.identity()));

        // Process each dispense request
        for (var req : request.getItems()) {
            PrescriptionItem item = itemMap.get(req.getPrescriptionItemId());
            if (item == null) {
                throw new IllegalArgumentException("Item #" + req.getPrescriptionItemId() + " not found");
            }

            int qty = req.getQuantity() != null ? req.getQuantity() : 0;
            if (qty <= 0)
                continue; // Skip zero quantities

            int prescribed = item.getQuantity() != null ? item.getQuantity() : 0;
            int alreadyDispensed = item.getDispensedQuantity() != null ? item.getDispensedQuantity() : 0;
            int newTotal = alreadyDispensed + qty;

            // Can't dispense more than prescribed
            if (newTotal > prescribed) {
                throw new IllegalArgumentException(String.format(
                        "Cannot dispense %d for %s (prescribed: %d, already dispensed: %d)",
                        qty, item.getMedicine().getName(), prescribed, alreadyDispensed));
            }

            // Update item
            item.setDispensedQuantity(newTotal);
            item.setDispensed(newTotal > 0);
            item.setDispensedAt(LocalDateTime.now());
        }

        // Check if all items are fully dispensed
        boolean allDone = rx.getItems().stream().allMatch(item -> {
            int p = item.getQuantity() != null ? item.getQuantity() : 0;
            int d = item.getDispensedQuantity() != null ? item.getDispensedQuantity() : 0;
            return p > 0 && d >= p;
        });

        rx.setStatus(allDone ? PrescriptionStatus.FILLED : PrescriptionStatus.PENDING);
        rx = prescriptionRepo.save(rx);

        // Update or create bill with current dispensed totals
        syncBill(rx);

        log.info("Dispensed prescription #{}, status: {}", id, rx.getStatus());
        return toDto(rx);
    }

    // ========== BILLING ==========

    /**
     * Sync bill to match current dispensed quantities.
     * Creates bill if doesn't exist, updates subtotal if pending.
     */
    private void syncBill(Prescription rx) {
        // Calculate subtotal from dispensed quantities
        BigDecimal subtotal = rx.getItems().stream()
                .filter(i -> i.getDispensedQuantity() != null && i.getDispensedQuantity() > 0)
                .map(i -> {
                    BigDecimal price = i.getMedicine().getPrice() != null
                            ? i.getMedicine().getPrice()
                            : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(i.getDispensedQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            return; // Nothing dispensed yet
        }

        Bill bill = rx.getBill();
        if (bill == null) {
            // Create new bill
            bill = Bill.builder()
                    .prescription(rx)
                    .status(BillStatus.PENDING)
                    .subtotal(subtotal)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .currency("RWF")
                    .paymentDate(LocalDateTime.now())
                    .build();
            billRepo.save(bill);
            log.info("Created bill for prescription #{}, subtotal: {}", rx.getId(), subtotal);
        } else if (bill.getStatus() == BillStatus.PENDING) {
            // Update existing pending bill
            bill.setSubtotal(subtotal);
            billRepo.save(bill);
            log.info("Updated bill #{} subtotal to {}", bill.getId(), subtotal);
        }
        // Don't modify paid bills
    }

    // ========== HELPERS ==========

    private Prescription findById(Long id) {
        return prescriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND + id));
    }

    private void checkPatientAccess(Prescription rx) {
        if (isPatient()) {
            Long myId = currentUserPersonId();
            if (myId == null || !rx.getPerson().getId().equals(myId)) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
    }

    private Specification<Prescription> searchSpec(String term) {
        return (root, query, cb) -> {
            String like = "%" + term.toLowerCase() + "%";
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(cb.lower(root.get("person").get("name")), like));
            preds.add(cb.like(cb.lower(root.get("pharmacist").get("name")), like));
            preds.add(cb.like(cb.lower(root.get("status").as(String.class)), like));
            Join<Prescription, Bill> billJoin = root.join("bill", JoinType.LEFT);
            try {
                Long id = Long.parseLong(term);
                preds.add(cb.equal(root.get("id"), id));
                preds.add(cb.equal(billJoin.get("id"), id));
                String idTerm = term.trim();
                preds.add(cb.like(cb.toString(root.get("id")), idTerm + "%"));
                preds.add(cb.like(cb.toString(billJoin.get("id")), idTerm + "%"));
            } catch (NumberFormatException ignored) {
            }
            try {
                LocalDate date = LocalDate.parse(term);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
                preds.add(cb.between(root.get("dateIssued"), start, end));
                preds.add(cb.between(billJoin.get("paymentDate"), start, end));
            } catch (Exception ignored) {
            }
            return cb.or(preds.toArray(new Predicate[0]));
        };
    }

    private Map<Long, Integer> aggregateCreateQuantities(List<PrescriptionCreateRequest.ItemRequest> items) {
        Map<Long, Integer> totals = new HashMap<>();
        if (items == null) {
            return totals;
        }
        for (var item : items) {
            if (item == null || item.getMedicineId() == null) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            totals.merge(item.getMedicineId(), qty, Integer::sum);
        }
        return totals;
    }

    private Map<Long, Integer> aggregateUpdateQuantities(List<PrescriptionUpdateRequest.ItemRequest> items) {
        Map<Long, Integer> totals = new HashMap<>();
        if (items == null) {
            return totals;
        }
        for (var item : items) {
            if (item == null || item.getMedicineId() == null) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            totals.merge(item.getMedicineId(), qty, Integer::sum);
        }
        return totals;
    }

    private Map<Long, Integer> aggregateExistingQuantities(List<PrescriptionItem> items) {
        Map<Long, Integer> totals = new HashMap<>();
        if (items == null) {
            return totals;
        }
        for (var item : items) {
            if (item == null || item.getMedicine() == null || item.getMedicine().getId() == null) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            totals.merge(item.getMedicine().getId(), qty, Integer::sum);
        }
        return totals;
    }

    private Map<Long, Integer> aggregateRemainingQuantities(List<PrescriptionItem> items) {
        Map<Long, Integer> totals = new HashMap<>();
        if (items == null) {
            return totals;
        }
        for (var item : items) {
            if (item == null || item.getMedicine() == null || item.getMedicine().getId() == null) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            int dispensed = item.getDispensedQuantity() != null ? item.getDispensedQuantity() : 0;
            int remaining = qty - dispensed;
            if (remaining <= 0) {
                continue;
            }
            totals.merge(item.getMedicine().getId(), remaining, Integer::sum);
        }
        return totals;
    }

    private Map<Long, Medicine> loadMedicines(Set<Long> ids) {
        Map<Long, Medicine> medicines = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return medicines;
        }
        List<Medicine> meds = medicineRepo.findAllById(ids);
        for (Medicine med : meds) {
            medicines.put(med.getId(), med);
        }
        if (medicines.size() != ids.size()) {
            for (Long id : ids) {
                if (!medicines.containsKey(id)) {
                    throw new IllegalArgumentException("Medicine not found with ID: " + id);
                }
            }
        }
        return medicines;
    }

    private void validateSufficientStock(Map<Long, Medicine> medicines, Map<Long, Integer> required) {
        if (required == null || required.isEmpty()) {
            return;
        }
        for (var entry : required.entrySet()) {
            Medicine med = medicines.get(entry.getKey());
            int available = med != null ? safeQty(med.getQuantity()) : 0;
            int qty = entry.getValue() != null ? entry.getValue() : 0;
            if (qty <= 0) {
                continue;
            }
            if (available < qty) {
                throw new IllegalStateException(String.format(
                        "Insufficient stock for %s (available: %d, requested: %d)",
                        med != null ? med.getName() : "medicine #" + entry.getKey(), available, qty));
            }
        }
    }

    private void applyStockDeltas(Map<Long, Medicine> medicines, Map<Long, Integer> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        for (var entry : deltas.entrySet()) {
            Medicine med = medicines.get(entry.getKey());
            if (med == null) {
                continue;
            }
            int available = safeQty(med.getQuantity());
            int delta = entry.getValue() != null ? entry.getValue() : 0;
            med.setQuantity(available - delta);
        }
    }

    private int safeQty(Integer qty) {
        return qty != null ? qty : 0;
    }

    private PrescriptionDto toDto(Prescription p) {
        return PrescriptionDto.builder()
                .id(p.getId())
                .dateIssued(p.getDateIssued())
                .status(p.getStatus() != null ? p.getStatus().name() : "PENDING")
                .personId(p.getPerson().getId())
                .patientName(p.getPerson().getName())
                .pharmacistId(p.getPharmacist().getId())
                .pharmacistName(p.getPharmacist().getName())
                .notes(p.getNotes())
                .walkInName(p.getWalkInName())
                .walkInPhone(p.getWalkInPhone())
                .items(p.getItems().stream()
                        .map(i -> PrescriptionItemDto.builder()
                                .id(i.getId())
                                .prescriptionId(p.getId())
                                .medicineId(i.getMedicine().getId())
                                .medicineName(i.getMedicine().getName())
                                .medicinePrice(i.getMedicine().getPrice())
                                .quantity(i.getQuantity())
                                .dispensedQuantity(i.getDispensedQuantity())
                                .dispensed(i.isDispensed())
                                .dosage(i.getDosage())
                                .frequency(i.getFrequency())
                                .duration(i.getDuration())
                                .instructions(i.getInstructions())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private boolean isPatient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PATIENT"::equals);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private boolean isPharmacist() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PHARMACIST"::equals);
    }

    private Long currentUserPersonId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null)
            return null;
        Person p = personRepo.findByEmail(auth.getName());
        return p != null ? p.getId() : null;
    }
}
