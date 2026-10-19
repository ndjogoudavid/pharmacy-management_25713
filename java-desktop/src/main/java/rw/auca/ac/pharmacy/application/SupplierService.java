package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.SupplierRepository;
import rw.auca.ac.pharmacy.domain.Supplier;
import java.util.List;

public final class SupplierService {
    private final SupplierRepository repository;
    public SupplierService(SupplierRepository repository) { this.repository = repository; }
    public List<Supplier> list() { return repository.findAll(); }
    public Supplier save(Supplier supplier) { return repository.save(supplier); }
    public void delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("Select a supplier first.");
        repository.deleteById(id);
    }
}
