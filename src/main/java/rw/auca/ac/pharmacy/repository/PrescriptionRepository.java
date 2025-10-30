package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);

    Page<Prescription> findByPharmacistId(Long pharmacistId, Pageable pageable);

    Page<Prescription> findByPersonId(Long personId, Pageable pageable);

    Optional<Prescription> findByIdAndStatus(Long id, PrescriptionStatus status);

    @NonNull Page<Prescription> findAll(@NonNull Pageable pageable);
}
