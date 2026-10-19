package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.SalesRepository;
import rw.auca.ac.pharmacy.domain.Sale;
import java.util.List;

public final class SalesService {
    private final SalesRepository repository;
    public SalesService(SalesRepository repository) { this.repository = repository; }
    public List<Sale> list() { return repository.findAll(); }
    public Sale sell(String customerName, String medicineName, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        return repository.sell(customerName, medicineName, quantity);
    }
    public void deleteAndRestock(int id) {
        if (id <= 0) throw new IllegalArgumentException("Select a sale first.");
        repository.deleteAndRestock(id);
    }
}
