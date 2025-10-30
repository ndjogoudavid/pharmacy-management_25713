package rw.auca.ac.pharmacy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import rw.auca.ac.pharmacy.model.core.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

    boolean existsByEmail(String email);
    Person findByEmail(String email);
    @NonNull Page<Person> findAll(@NonNull Pageable pageable);

    // ✅ Fixed: explicit query avoids property path parsing errors
    @Query("""
        SELECT p FROM Person p
        JOIN p.village v
        JOIN v.cell c
        JOIN c.sector s
        JOIN s.district d
        JOIN d.province pr
        WHERE LOWER(pr.name) = LOWER(:provinceName)
    """)
    Page<Person> findByProvinceName(@Param("provinceName") String provinceName, Pageable pageable);

    @Query("""
        SELECT p FROM Person p
        JOIN p.village v
        JOIN v.cell c
        JOIN c.sector s
        JOIN s.district d
        JOIN d.province pr
        WHERE pr.code = :provinceCode
    """)
    Page<Person> findByProvinceCode(@Param("provinceCode") String provinceCode, Pageable pageable);
}
