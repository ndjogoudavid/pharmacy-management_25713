package rw.auca.ac.pharmacy.repository.location;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.location.Location;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {

    boolean existsByCode(String code);

    Optional<Location> findByCode(String code);

    List<Location> findByParentId(Long parentId);

    Page<Location> findByParentId(Long parentId, Pageable pageable);

    Page<Location> findByParentIdAndType(Long parentId, LocationType type, Pageable pageable);

    List<Location> findByType(LocationType type);

    Page<Location> findByType(LocationType type, Pageable pageable);

    Page<Location> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Location> findByNameContainingIgnoreCaseAndType(String name, LocationType type, Pageable pageable);

    Page<Location> findByNameContainingIgnoreCaseAndParentIdAndType(String name, Long parentId, LocationType type, Pageable pageable);

    Page<Location> findByNameContainingIgnoreCaseAndParentId(String name, Long parentId, Pageable pageable);

    Page<Location> findAllByOrderByNameAsc(Pageable pageable);
}
