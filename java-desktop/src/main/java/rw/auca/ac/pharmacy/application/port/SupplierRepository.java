package rw.auca.ac.pharmacy.application.port;

import rw.auca.ac.pharmacy.domain.Supplier;
import java.util.List;

public interface SupplierRepository {
    List<Supplier> findAll();
    Supplier save(Supplier supplier);
    void deleteById(int id);
}
