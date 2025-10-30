package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.auca.ac.pharmacy.model.core.Medicine;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // Simple list search (for getLowStock() and search())
    List<Medicine> findByNameContainingIgnoreCase(String name);
    List<Medicine> findByQuantityLessThan(int reorderLevel);

    // Paginated variants (for APIs)
    Page<Medicine> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Medicine> findByQuantityLessThan(int reorderLevel, Pageable pageable);

    Page<Medicine> findAll(Pageable pageable);
}
