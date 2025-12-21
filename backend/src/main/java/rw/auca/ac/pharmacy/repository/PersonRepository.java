package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import rw.auca.ac.pharmacy.model.core.Person;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

  boolean existsByEmail(String email);

  Person findByEmail(String email);

  Person findByResetToken(String token);

  // Default sorted pagination
  @NonNull
  Page<Person> findAllByOrderByNameAsc(@NonNull Pageable pageable);

  Page<Person> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(String name, String email,
      Pageable pageable);

  // Simple list-based search for global search (no pagination needed)
  List<Person> findTop20ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(String name, String email);

  Page<Person> findByRoleOrderByNameAsc(rw.auca.ac.pharmacy.model.enums.Role role, Pageable pageable);

  long countByRole(rw.auca.ac.pharmacy.model.enums.Role role);

  // Filter by province name (sorted)
  @Query("""
          SELECT p FROM Person p
          LEFT JOIN p.location l0
          LEFT JOIN l0.parent l1
          LEFT JOIN l1.parent l2
          LEFT JOIN l2.parent l3
          LEFT JOIN l3.parent l4
          WHERE (
            (l0.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND LOWER(l0.name) = LOWER(:provinceName)) OR
            (l1.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND LOWER(l1.name) = LOWER(:provinceName)) OR
            (l2.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND LOWER(l2.name) = LOWER(:provinceName)) OR
            (l3.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND LOWER(l3.name) = LOWER(:provinceName)) OR
            (l4.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND LOWER(l4.name) = LOWER(:provinceName))
          )
          ORDER BY p.name ASC
      """)
  Page<Person> findByProvinceName(@Param("provinceName") String provinceName, Pageable pageable);

  // Filter by province code (sorted)
  @Query("""
          SELECT p FROM Person p
          LEFT JOIN p.location l0
          LEFT JOIN l0.parent l1
          LEFT JOIN l1.parent l2
          LEFT JOIN l2.parent l3
          LEFT JOIN l3.parent l4
          WHERE (
            (l0.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND l0.code = :provinceCode) OR
            (l1.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND l1.code = :provinceCode) OR
            (l2.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND l2.code = :provinceCode) OR
            (l3.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND l3.code = :provinceCode) OR
            (l4.type = rw.auca.ac.pharmacy.model.enums.LocationType.PROVINCE AND l4.code = :provinceCode)
          )
          ORDER BY p.name ASC
      """)
  Page<Person> findByProvinceCode(@Param("provinceCode") String provinceCode, Pageable pageable);
}
