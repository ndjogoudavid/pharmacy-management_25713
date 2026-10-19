package rw.auca.ac.pharmacy.application.port;

import rw.auca.ac.pharmacy.domain.Sale;
import java.util.List;

public interface SalesRepository {
    List<Sale> findAll();
    Sale sell(String customerName, String medicineName, int quantity);
    void deleteAndRestock(int saleId);
}
