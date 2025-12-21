package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long>, JpaSpecificationExecutor<Prescription> {

    Page<Prescription> findByStatusOrderByDateIssuedDesc(PrescriptionStatus status, Pageable pageable);

    Page<Prescription> findByPharmacistIdOrderByDateIssuedDesc(Long pharmacistId, Pageable pageable);

    Page<Prescription> findByPersonIdOrderByDateIssuedDesc(Long personId, Pageable pageable);

    Optional<Prescription> findByIdAndStatus(Long id, PrescriptionStatus status);

    @NonNull
    Page<Prescription> findAllByOrderByDateIssuedDesc(@NonNull Pageable pageable);
    long countByStatus(PrescriptionStatus status);
    long countByPersonId(Long personId);
    long countByPersonIdAndStatus(Long personId, PrescriptionStatus status);
    long countByPharmacistId(Long pharmacistId);
    long countByPharmacistIdAndStatus(Long pharmacistId, PrescriptionStatus status);

    @Query("""
        SELECT pr FROM Prescription pr
        JOIN pr.person pe
        WHERE (:status IS NULL OR pr.status = :status)
          AND (:patientName IS NULL OR LOWER(pe.name) LIKE LOWER(CONCAT('%', :patientName, '%')))
        ORDER BY pr.dateIssued DESC
    """)
    Page<Prescription> searchByStatusAndPatientName(@Param("status") PrescriptionStatus status,
                                                    @Param("patientName") String patientName,
                                                    Pageable pageable);

    @Query("""
        SELECT pr FROM Prescription pr
        JOIN pr.person pe
        WHERE LOWER(COALESCE(pr.notes,'')) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(pe.name) LIKE LOWER(CONCAT('%', :term, '%'))
        ORDER BY pr.dateIssued DESC
    """)
    Page<Prescription> searchForTerm(@Param("term") String term, Pageable pageable);
}
