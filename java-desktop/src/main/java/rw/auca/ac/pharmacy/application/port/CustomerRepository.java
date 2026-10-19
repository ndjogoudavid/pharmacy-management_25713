package rw.auca.ac.pharmacy.application.port;

import rw.auca.ac.pharmacy.domain.Customer;
import java.util.List;

public interface CustomerRepository {
    List<Customer> findAll();
    void save(Customer customer, String originalName);
    void deleteByName(String name);
}
