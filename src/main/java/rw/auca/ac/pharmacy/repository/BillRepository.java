package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.auca.ac.pharmacy.model.core.Bill;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Bill findByPrescriptionId(Long prescriptionId);

    Page<Bill> findByStatus(String status, Pageable pageable);

    Page<Bill> findAll(Pageable pageable);
}
