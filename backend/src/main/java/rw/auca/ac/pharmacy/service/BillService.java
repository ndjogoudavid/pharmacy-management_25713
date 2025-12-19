package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.BillDto;
import rw.auca.ac.pharmacy.dto.BillRequest;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BillService {
    private static final String BILL_NOT_FOUND_MSG = "Bill not found with ID: ";
    private static final String PRESCRIPTION_NOT_FOUND_MSG = "Prescription not found with ID: ";

    private final BillRepository repository;
    private final PrescriptionRepository prescriptionRepo;
    private final PersonRepository personRepository;

    private static class BillSpecifications {
        public static Specification<Bill> hasStatus(BillStatus status) {
            return (root, query, cb) -> cb.equal(root.get("status"), status);
        }
        public static Specification<Bill> hasPaymentMethod(PaymentMethod method) {
            return (root, query, cb) -> cb.equal(root.get("paymentMethod"), method);
        }
        public static Specification<Bill> hasPaymentRef(String paymentRef) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("paymentRef")), "%" + paymentRef.toLowerCase() + "%");
        }
        public static Specification<Bill> hasPrescriptionId(Long prescriptionId) {
            return (root, query, cb) -> cb.equal(root.get("prescription").get("id"), prescriptionId);
        }
        public static Specification<Bill> hasPersonId(Long personId) {
            return (root, query, cb) -> cb.equal(root.get("prescription").get("person").get("id"), personId);
        }
        public static Specification<Bill> hasPatientName(String patientName) {
            return (root, query, cb) -> {
                String pattern = "%" + patientName.toLowerCase() + "%";
                return cb.like(cb.lower(root.get("prescription").get("person").get("name")), pattern);
            };
        }
    }

    @Transactional
    public BillDto createForPrescription(Long prescriptionId, BillRequest request) {
        if (repository.existsByPrescriptionId(prescriptionId)) {
            throw new IllegalArgumentException("Bill already exists for prescription ID: " + prescriptionId);
        }

        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND_MSG + prescriptionId));

        BigDecimal subtotal = prescription.getItems().stream()
                .filter(rw.auca.ac.pharmacy.model.core.PrescriptionItem::isDispensed)
                .map(item -> {
                    BigDecimal price = item.getMedicine().getPrice() != null
                            ? item.getMedicine().getPrice()
                            : BigDecimal.ZERO;
                    return BigDecimal.valueOf(item.getDispensedQuantity()).multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BillStatus status = request.getStatus() != null && !request.getStatus().isBlank()
                ? BillStatus.valueOf(request.getStatus())
                : BillStatus.PENDING;
        PaymentMethod method = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? PaymentMethod.valueOf(request.getPaymentMethod())
                : null;

        Bill bill = Bill.builder()
                .prescription(prescription)
                .status(status)
                .paymentMethod(method)
                .paymentRef(request.getPaymentRef())
                .subtotal(subtotal)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .currency("RWF")
                .paymentDate(request.getPaymentDate())
                .build();

        Bill saved = repository.save(bill);
        log.info("✅ Created bill ID {} for prescription {}", saved.getId(), prescriptionId);
        return toDto(saved);
    }

    public BillDto get(Long id) {
        Bill bill = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND_MSG + id));
        if (isPatient()) {
            Long me = currentUserPersonId();
            if (me == null || !bill.getPrescription().getPerson().getId().equals(me)) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
        log.debug("Retrieved bill ID {}", id);
        return toDto(bill);
    }

    @Transactional
    public BillDto getByPrescription(Long prescriptionId) {
        Bill bill = repository.findByPrescriptionId(prescriptionId).orElse(null);

        if (bill == null) {
            Prescription prescription = prescriptionRepo.findById(prescriptionId)
                    .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND_MSG + prescriptionId));

            if (prescription.getItems().stream().noneMatch(rw.auca.ac.pharmacy.model.core.PrescriptionItem::isDispensed)) {
                throw new IllegalArgumentException("No dispensed items in this prescription; no bill exists");
            }

            BigDecimal subtotal = prescription.getItems().stream()
                    .filter(rw.auca.ac.pharmacy.model.core.PrescriptionItem::isDispensed)
                    .map(item -> {
                        BigDecimal price = item.getMedicine().getPrice() != null
                                ? item.getMedicine().getPrice()
                                : BigDecimal.ZERO;
                        return BigDecimal.valueOf(item.getDispensedQuantity()).multiply(price);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            bill = Bill.builder()
                    .prescription(prescription)
                    .status(BillStatus.PENDING)
                    .paymentMethod(null)
                    .paymentRef(null)
                    .subtotal(subtotal)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .currency("RWF")
                    .paymentDate(LocalDateTime.now())
                    .build();

            bill = repository.save(bill);
            log.info("Auto-created bill ID {} for prescription {}", bill.getId(), prescriptionId);
        }

        if (isPatient()) {
            Long me = currentUserPersonId();
            if (me == null || !bill.getPrescription().getPerson().getId().equals(me)) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
        log.debug("Retrieved bill for prescription ID {}", prescriptionId);
        return toDto(bill);
    }

    public Page<BillDto> getAll(Pageable pageable, Long prescriptionId, BillStatus status, PaymentMethod paymentMethod, String paymentRef, String patientName) {
        Specification<Bill> spec = Specification.where(null);

        if (isPatient()) {
            Long me = currentUserPersonId();
            if (me == null) return Page.empty(pageable);
            spec = spec.and(BillSpecifications.hasPersonId(me));
        }

        if (prescriptionId != null) {
            spec = spec.and(BillSpecifications.hasPrescriptionId(prescriptionId));
        }
        if (status != null) {
            spec = spec.and(BillSpecifications.hasStatus(status));
        }
        if (paymentMethod != null) {
            spec = spec.and(BillSpecifications.hasPaymentMethod(paymentMethod));
        }
        if (paymentRef != null && !paymentRef.isBlank()) {
            spec = spec.and(BillSpecifications.hasPaymentRef(paymentRef.trim()));
        }
        if (patientName != null && !patientName.isBlank()) {
            spec = spec.and(BillSpecifications.hasPatientName(patientName.trim()));
        }

        return repository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public BillDto update(Long id, BillRequest request) {
        Bill bill = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND_MSG + id));

        if (request.getStatus() != null) {
            bill.setStatus(BillStatus.valueOf(request.getStatus()));
        }
        if (request.getPaymentMethod() != null) {
            bill.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        }
        bill.setPaymentRef(request.getPaymentRef());
        if (request.getDiscountAmount() != null) {
            bill.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getTaxAmount() != null) {
            bill.setTaxAmount(request.getTaxAmount());
        }
        bill.setPaymentDate(request.getPaymentDate());

        Bill updated = repository.save(bill);
        log.info("✅ Updated bill ID {}", id);
        return toDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Bill bill = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND_MSG + id));
        bill.setDeletedAt(LocalDateTime.now());
        repository.save(bill);
        log.warn("🗑️ Soft-deleted bill ID {}", id);
    }

    private BillDto toDto(Bill bill) {
        return BillDto.builder()
                .id(bill.getId())
                .prescriptionId(bill.getPrescription().getId())
                .status(bill.getStatus() != null ? bill.getStatus().name() : BillStatus.PENDING.name())
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

    public boolean existsForPrescription(Long prescriptionId) {
        return repository.existsByPrescriptionId(prescriptionId);
    }

    private boolean isPatient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_PATIENT"));
    }

    private Long currentUserPersonId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        var p = personRepository.findByEmail(auth.getName());
        return p != null ? p.getId() : null;
    }
}
