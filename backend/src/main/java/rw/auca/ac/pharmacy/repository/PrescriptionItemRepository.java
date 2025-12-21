package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    Page<PrescriptionItem> findByPrescriptionId(Long prescriptionId, Pageable pageable);

    Page<PrescriptionItem> findByMedicineId(Long medicineId, Pageable pageable);
}
