package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.CustomerRepository;
import rw.auca.ac.pharmacy.domain.Customer;
import java.util.List;

public final class CustomerService {
    private final CustomerRepository repository;
    public CustomerService(CustomerRepository repository) { this.repository = repository; }
    public List<Customer> list() { return repository.findAll(); }
    public void save(Customer customer, String originalName) { repository.save(customer, originalName); }
    public void delete(String name) { repository.deleteByName(CustomerName.require(name)); }

    private static final class CustomerName {
        static String require(String value) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Select a customer first.");
            return value.trim();
        }
    }
}
