package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rw.auca.ac.pharmacy.model.core.Medicine;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {

    // Search (list + pagination)
    List<Medicine> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    Page<Medicine> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    Page<Medicine> findBySkuContainingIgnoreCaseOrderByNameAsc(String sku, Pageable pageable);

    List<Medicine> findByQuantityLessThanOrderByNameAsc(int reorderLevel);
    Page<Medicine> findByQuantityLessThanOrderByNameAsc(int reorderLevel, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM Medicine m
        WHERE m.reorderLevel IS NOT NULL
          AND m.quantity < m.reorderLevel
        ORDER BY m.name ASC
    """)
    List<Medicine> findLowStockByOwnThreshold();

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(m) FROM Medicine m
        WHERE m.reorderLevel IS NOT NULL
          AND m.quantity <= m.reorderLevel
    """)
    long countLowStockByOwnThreshold();

    Page<Medicine> findAllByOrderByNameAsc(Pageable pageable);

    long countByQuantityLessThanEqual(int threshold);
}
