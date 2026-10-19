package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.MedicineRepository;
import rw.auca.ac.pharmacy.domain.Medicine;
import java.util.List;

public final class MedicineService {
    private final MedicineRepository repository;
    public MedicineService(MedicineRepository repository) { this.repository = repository; }
    public List<Medicine> list() { return repository.findAll(); }
    public Medicine findByName(String name) {
        return repository.findByName(Medicine.required(name, "Medicine name"))
                .orElseThrow(() -> new IllegalArgumentException("Medicine was not found."));
    }
    public Medicine save(Medicine medicine) {
        boolean nameExists = repository.findAll().stream().anyMatch(existing -> existing.id() != medicine.id()
                && existing.name().equalsIgnoreCase(medicine.name()));
        if (nameExists) throw new IllegalArgumentException("A medicine with this name already exists.");
        return repository.save(medicine);
    }
    public void delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("Select a medicine first.");
        repository.deleteById(id);
    }
}
