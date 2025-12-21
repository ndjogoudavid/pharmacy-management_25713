package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rw.auca.ac.pharmacy.model.core.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    Page<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    // Simple list-based search for global search (no pagination needed)
    List<Category> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String name);

    Page<Category> findByNameContainingIgnoreCaseAndDescriptionContainingIgnoreCaseOrderByNameAsc(String name,
            String description, Pageable pageable);

    Page<Category> findByDescriptionContainingIgnoreCaseOrderByNameAsc(String description, Pageable pageable);

    Page<Category> findAllByOrderByNameAsc(Pageable pageable);
}
