package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {
    Optional<Bill> findByPrescriptionId(Long prescriptionId);

    Page<Bill> findAllByOrderByPaymentDateDesc(Pageable pageable);

    Page<Bill> findByStatusOrderByPaymentDateDesc(BillStatus status, Pageable pageable);

    boolean existsByPrescriptionId(Long prescriptionId);
    long countByStatus(BillStatus status);

    @Query("SELECT COALESCE(SUM(b.subtotal - b.discountAmount + b.taxAmount),0) FROM Bill b WHERE b.status = :status")
    java.math.BigDecimal sumNetByStatus(@Param("status") BillStatus status);

    @Query("""
        SELECT COALESCE(SUM(b.subtotal - b.discountAmount + b.taxAmount),0)
        FROM Bill b
        WHERE b.status = :status
          AND b.prescription.person.id = :personId
    """)
    BigDecimal sumNetByStatusForPerson(@Param("status") BillStatus status, @Param("personId") Long personId);

    Page<Bill> findByStatusAndPaymentMethodOrderByPaymentDateDesc(BillStatus status, PaymentMethod method, Pageable pageable);

    Page<Bill> findByPaymentRefContainingIgnoreCaseOrderByPaymentDateDesc(String ref, Pageable pageable);

    Page<Bill> findByPrescriptionPersonIdOrderByPaymentDateDesc(Long personId, Pageable pageable);

    long countByPrescriptionPersonId(Long personId);

    long countByPrescriptionPersonIdAndStatus(Long personId, BillStatus status);

    Page<Bill> findByPrescriptionPharmacistIdOrderByPaymentDateDesc(Long pharmacistId, Pageable pageable);

    long countByPrescriptionPharmacistId(Long pharmacistId);

    long countByPrescriptionPharmacistIdAndStatus(Long pharmacistId, BillStatus status);

    @Query("""
        SELECT COALESCE(SUM(b.subtotal - b.discountAmount + b.taxAmount),0)
        FROM Bill b
        WHERE b.status = :status
          AND b.prescription.pharmacist.id = :pharmacistId
    """)
    BigDecimal sumNetByStatusForPharmacist(@Param("status") BillStatus status,
                                           @Param("pharmacistId") Long pharmacistId);
}
