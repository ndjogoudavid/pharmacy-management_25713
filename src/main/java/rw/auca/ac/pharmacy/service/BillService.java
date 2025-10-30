package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.BillDto;
import rw.auca.ac.pharmacy.dto.BillRequest;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;  // Fixed: Added import

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillService {
    private static final String BILL_NOT_FOUND_MSG = "Bill not found with ID: ";

    private final BillRepository repository;
    private final PrescriptionRepository prescriptionRepo;

    public BillDto createForPrescription(Long prescriptionId, BillRequest request) {
        Prescription p = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + prescriptionId));

        BigDecimal subtotal = p.getItems().stream()
                .map(item -> BigDecimal.valueOf(item.getQuantity()).multiply(item.getMedicine().getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BillStatus status = request.getStatus() != null ? BillStatus.valueOf(request.getStatus()) : BillStatus.PENDING;
        PaymentMethod method = request.getPaymentMethod() != null ? PaymentMethod.valueOf(request.getPaymentMethod()) : null;

        Bill bill = Bill.builder()
                .prescription(p)
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
        return toDto(bill);
    }

    public BillDto update(Long id, BillRequest request) {
        Bill bill = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND_MSG + id));

        BillStatus status = request.getStatus() != null ? BillStatus.valueOf(request.getStatus()) : bill.getStatus();
        PaymentMethod method = request.getPaymentMethod() != null ? PaymentMethod.valueOf(request.getPaymentMethod()) : bill.getPaymentMethod();

        bill.setStatus(status);
        bill.setPaymentMethod(method);
        bill.setPaymentRef(request.getPaymentRef());
        bill.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : bill.getDiscountAmount());
        bill.setTaxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : bill.getTaxAmount());
        bill.setPaymentDate(request.getPaymentDate());

        Bill updated = repository.save(bill);
        log.info("✅ Updated bill ID {}", id);
        return toDto(updated);
    }

    public void delete(Long id) {
        Bill bill = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(BILL_NOT_FOUND_MSG + id));
        bill.setDeletedAt(LocalDateTime.now());  // Fixed: Now resolves
        repository.save(bill);
        log.warn("🗑️ Soft-deleted bill ID {}", id);
    }

    private BillDto toDto(Bill bill) {
        return BillDto.builder()
                .id(bill.getId())
                .prescriptionId(bill.getPrescription().getId())
                .status(bill.getStatus().name())
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
}