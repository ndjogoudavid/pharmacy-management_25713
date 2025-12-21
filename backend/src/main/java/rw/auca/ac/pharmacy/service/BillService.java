package rw.auca.ac.pharmacy.service;

import jakarta.persistence.EntityNotFoundException;
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
import rw.auca.ac.pharmacy.dto.BillDto;
import rw.auca.ac.pharmacy.dto.BillRequest;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bill management for pharmacy prescriptions.
 * 
 * Real-world flow:
 * 1. Bill is auto-created when pharmacist first dispenses items (via
 * PrescriptionService)
 * 2. Bill subtotal updates automatically with each dispense
 * 3. Patient pays bill after prescription is fully filled
 * 4. Paid bills cannot be modified
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BillService {

    private static final String BILL_NOT_FOUND = "Bill not found with ID: ";

    private final BillRepository billRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final PersonRepository personRepo;

    // ========== CREATE OPERATIONS ==========

    /**
     * Create a new bill for a prescription.
     * Used when manually creating bills from the Bills page.
     */
    @Transactional
    public BillDto createForPrescription(@NonNull Long prescriptionId, BillRequest request) {
        if (billRepo.existsByPrescriptionId(prescriptionId)) {
            throw new IllegalArgumentException("Bill already exists for prescription #" + prescriptionId);
        }

        Prescription rx = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        BillStatus status = request.getStatus() != null && !request.getStatus().isBlank()
                ? BillStatus.valueOf(request.getStatus())
                : BillStatus.PENDING;

        PaymentMethod method = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? PaymentMethod.valueOf(request.getPaymentMethod())
                : null;

        // PAID requires FILLED prescription and payment method
        if (status == BillStatus.PAID) {
            if (method == null) {
                throw new IllegalArgumentException("Payment method is required for PAID status");
            }
            if (rx.getStatus() != PrescriptionStatus.FILLED) {
                throw new IllegalStateException("Cannot pay: prescription must be fully dispensed first");
            }
        }

        BigDecimal subtotal = calculateSubtotal(rx);

        Bill bill = Bill.builder()
                .prescription(rx)
                .status(status)
                .paymentMethod(method)
                .paymentRef(request.getPaymentRef())
                .subtotal(subtotal)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .currency("RWF")
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now())
                .build();

        bill = billRepo.save(bill);
        log.info("Created bill #{} for prescription #{}", bill.getId(), prescriptionId);
        return toDto(bill);
    }

    // ========== GET OPERATIONS ==========

    public BillDto get(@NonNull Long id) {
        Bill bill = findById(id);
        checkPatientAccess(bill);
        return toDto(bill);
    }

    /**
     * Get bill for a prescription. Creates one if it doesn't exist.
     * This is used by the frontend to view/create bills.
     */
    @Transactional
    public BillDto getByPrescription(@NonNull Long prescriptionId) {
        Bill bill = billRepo.findByPrescriptionId(prescriptionId).orElse(null);

        if (bill == null) {
            Prescription rx = prescriptionRepo.findById(prescriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

            // Calculate subtotal from dispensed items
            BigDecimal subtotal = calculateSubtotal(rx);

            bill = Bill.builder()
                    .prescription(rx)
                    .status(BillStatus.PENDING)
                    .subtotal(subtotal)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .currency("RWF")
                    .paymentDate(LocalDateTime.now())
                    .build();
            bill = billRepo.save(bill);
            log.info("Created bill #{} for prescription #{}", bill.getId(), prescriptionId);
        } else if (bill.getStatus() == BillStatus.PENDING) {
            // Refresh subtotal for pending bills
            BigDecimal subtotal = calculateSubtotal(bill.getPrescription());
            if (subtotal.compareTo(bill.getSubtotal()) != 0) {
                bill.setSubtotal(subtotal);
                bill = billRepo.save(bill);
            }
        }

        checkPatientAccess(bill);
        return toDto(bill);
    }

    public Page<BillDto> getAll(@NonNull Pageable pageable, String id, String prescriptionId, BillStatus status,
            PaymentMethod paymentMethod, String paymentRef, String patientName, String q) {

        Specification<Bill> spec = Specification.where(null);

        // Patients can only see their own bills
        if (isPatient()) {
            Long myId = currentUserPersonId();
            if (myId == null)
                return Page.empty(pageable);
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("prescription").get("person").get("id"), myId));
        } else if (isPharmacist() && !isAdmin()) {
            Long myId = currentUserPersonId();
            if (myId == null)
                return Page.empty(pageable);
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("prescription").get("pharmacist").get("id"), myId));
        }

        // Apply filters
        if (id != null && !id.isBlank()) {
            String idTerm = id.trim();
            spec = spec.and((r, cq, cb) -> cb.like(cb.toString(r.get("id")), idTerm + "%"));
        }
        if (prescriptionId != null && !prescriptionId.isBlank()) {
            String rxTerm = prescriptionId.trim();
            spec = spec.and((r, cq, cb) -> cb.like(cb.toString(r.get("prescription").get("id")), rxTerm + "%"));
        }
        if (status != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("status"), status));
        }
        if (paymentMethod != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("paymentMethod"), paymentMethod));
        }
        if (paymentRef != null && !paymentRef.isBlank()) {
            spec = spec.and((r, cq, cb) -> cb.like(cb.lower(r.get("paymentRef")),
                    "%" + paymentRef.toLowerCase() + "%"));
        }
        if (patientName != null && !patientName.isBlank()) {
            String like = "%" + patientName.toLowerCase() + "%";
            spec = spec.and((r, cq, cb) -> cb.or(
                    cb.like(cb.lower(r.get("prescription").get("person").get("name")), like),
                    cb.like(cb.lower(cb.coalesce(r.get("prescription").get("walkInName"), "")), like),
                    cb.like(cb.lower(cb.coalesce(r.get("prescription").get("walkInPhone"), "")), like)
            ));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and(searchSpec(q.trim()));
        }

        return billRepo.findAll(spec, pageable).map(this::toDto);
    }

    // ========== UPDATE OPERATIONS ==========

    /**
     * Update bill - primarily used for payment processing.
     * 
     * Key rules:
     * - Paid bills cannot be modified at all
     * - Can only mark as PAID if prescription is FILLED
     * - PAID status requires a payment method
     * - Payment date auto-sets to NOW when marking as PAID
     */
    @Transactional
    public BillDto update(@NonNull Long id, BillRequest request) {
        Bill bill = findById(id);

        // PAID bills are completely locked - no modifications allowed
        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Cannot modify a paid bill");
        }

        // Determine new status and payment method
        BillStatus newStatus = request.getStatus() != null && !request.getStatus().isBlank()
                ? BillStatus.valueOf(request.getStatus())
                : bill.getStatus();

        PaymentMethod newMethod = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? PaymentMethod.valueOf(request.getPaymentMethod())
                : bill.getPaymentMethod();

        // Enforce business rules for PAID status
        if (newStatus == BillStatus.PAID) {
            if (newMethod == null) {
                throw new IllegalArgumentException("Payment method is required to mark bill as paid");
            }
            if (request.getPaymentRef() == null || request.getPaymentRef().isBlank()) {
                throw new IllegalArgumentException("Payment reference is required to mark bill as paid");
            }

            Prescription rx = bill.getPrescription();
            if (rx.getStatus() != PrescriptionStatus.FILLED) {
                throw new IllegalStateException("Cannot pay: prescription must be fully dispensed first");
            }

            // Lock in final subtotal at payment time
            bill.setSubtotal(calculateSubtotal(rx));

            // Auto-set payment date to NOW - not user-editable
            bill.setPaymentDate(LocalDateTime.now());
        }

        // Apply updates
        bill.setStatus(newStatus);
        bill.setPaymentMethod(newMethod);

        if (request.getPaymentRef() != null) {
            bill.setPaymentRef(request.getPaymentRef());
        }
        if (request.getDiscountAmount() != null) {
            bill.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getTaxAmount() != null) {
            bill.setTaxAmount(request.getTaxAmount());
        }
        // Payment date is NOT taken from request - auto-set when PAID

        bill = billRepo.save(bill);
        log.info("Updated bill #{} status: {}", id, newStatus);
        return toDto(bill);
    }

    @Transactional
    public void delete(@NonNull Long id) {
        Bill bill = findById(id);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Cannot delete a paid bill");
        }
        bill.setDeletedAt(LocalDateTime.now());
        billRepo.save(bill);
        log.warn("Soft-deleted bill #{}", id);
    }

    // ========== HELPERS ==========

    private Bill findById(Long id) {
        return billRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND + id));
    }

    private void checkPatientAccess(Bill bill) {
        if (isPatient()) {
            Long myId = currentUserPersonId();
            if (myId == null || !bill.getPrescription().getPerson().getId().equals(myId)) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
    }

    /**
     * Calculate subtotal from dispensed quantities.
     * Only charges for what has actually been dispensed.
     */
    private BigDecimal calculateSubtotal(Prescription rx) {
        return rx.getItems().stream()
                .filter(i -> i.getDispensedQuantity() != null && i.getDispensedQuantity() > 0)
                .map(i -> {
                    BigDecimal price = i.getMedicine().getPrice() != null
                            ? i.getMedicine().getPrice()
                            : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(i.getDispensedQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Specification<Bill> searchSpec(String term) {
        return (root, query, cb) -> {
            String like = "%" + term.toLowerCase() + "%";
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(cb.lower(cb.coalesce(root.get("paymentRef"), "")), like));
            preds.add(cb.like(cb.lower(root.get("status").as(String.class)), like));
            preds.add(cb.like(cb.lower(cb.coalesce(root.get("paymentMethod").as(String.class), "")), like));
            preds.add(cb.like(cb.lower(root.get("prescription").get("person").get("name")), like));
            preds.add(cb.like(cb.lower(cb.coalesce(root.get("prescription").get("walkInName"), "")), like));
            preds.add(cb.like(cb.lower(cb.coalesce(root.get("prescription").get("walkInPhone"), "")), like));
            try {
                Long id = Long.parseLong(term);
                preds.add(cb.equal(root.get("id"), id));
                preds.add(cb.equal(root.get("prescription").get("id"), id));
                String idTerm = term.trim();
                preds.add(cb.like(cb.toString(root.get("id")), idTerm + "%"));
                preds.add(cb.like(cb.toString(root.get("prescription").get("id")), idTerm + "%"));
            } catch (NumberFormatException ignored) {
            }
            try {
                BigDecimal amount = new BigDecimal(term);
                preds.add(cb.equal(root.get("subtotal"), amount));
                preds.add(cb.equal(root.get("netTotal"), amount));
            } catch (NumberFormatException ignored) {
            }
            return cb.or(preds.toArray(new Predicate[0]));
        };
    }

    private BillDto toDto(Bill bill) {
        Person patient = null;
        Long rxId = null;
        String walkInName = null;
        String walkInPhone = null;

        try {
            Prescription rx = bill.getPrescription();
            if (rx != null) {
                rxId = rx.getId();
                patient = rx.getPerson();
                walkInName = rx.getWalkInName();
                walkInPhone = rx.getWalkInPhone();
            }
        } catch (EntityNotFoundException e) {
            log.warn("Bill #{} has missing prescription", bill.getId());
        }

        return BillDto.builder()
                .id(bill.getId())
                .prescriptionId(rxId)
                .patientId(patient != null ? patient.getId() : null)
                .patientName(patient != null ? patient.getName() : null)
                .customerName(walkInName)
                .customerPhone(walkInPhone)
                .status(bill.getStatus() != null ? bill.getStatus().name() : "PENDING")
                .paymentMethod(bill.getPaymentMethod() != null ? bill.getPaymentMethod().name() : null)
                .paymentRef(bill.getPaymentRef())
                .subtotal(bill.getSubtotal())
                .discountAmount(bill.getDiscountAmount())
                .taxAmount(bill.getTaxAmount())
                .netTotal(bill.getNetTotal())
                .currency(bill.getCurrency())
                .paymentDate(bill.getPaymentDate())
                .build();
    }

    public boolean existsForPrescription(@NonNull Long prescriptionId) {
        return billRepo.existsByPrescriptionId(prescriptionId);
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
