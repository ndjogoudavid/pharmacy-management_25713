package rw.auca.ac.pharmacy.application.port;

import rw.auca.ac.pharmacy.domain.Medicine;
import java.util.List;
import java.util.Optional;

public interface MedicineRepository {
    List<Medicine> findAll();
    Optional<Medicine> findByName(String name);
    Medicine save(Medicine medicine);
    void deleteById(int id);
}
