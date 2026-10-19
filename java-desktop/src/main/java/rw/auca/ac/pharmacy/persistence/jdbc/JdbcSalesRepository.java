package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.SalesRepository;
import rw.auca.ac.pharmacy.domain.Sale;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcSalesRepository implements SalesRepository {
    private final Database database;
    public JdbcSalesRepository(Database database) { this.database = database; }

    @Override public List<Sale> findAll() {
        return database.query(connection -> {
            List<Sale> rows = new ArrayList<>();
            try (PreparedStatement s = connection.prepareStatement(
                    "SELECT id, sale_date, customer_name, medicine_name, quantity, unit_price, total_amount FROM sales ORDER BY sale_date DESC, id DESC");
                 ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new Sale(r.getInt("id"), r.getTimestamp("sale_date").toLocalDateTime(),
                        r.getString("customer_name"), r.getString("medicine_name"), r.getInt("quantity"),
                        r.getBigDecimal("unit_price"), r.getBigDecimal("total_amount")));
            }
            return rows;
        });
    }

    @Override public Sale sell(String customerName, String medicineName, int quantity) {
        return database.transaction(connection -> {
            int medicineId; int stock; java.math.BigDecimal price; String storedName;
            try (PreparedStatement s = connection.prepareStatement(
                    "SELECT ID, Name, Quantity, Price FROM inventory WHERE Name=? ORDER BY ID FOR UPDATE")) {
                s.setString(1, medicineName.trim());
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("Medicine was not found in inventory.");
                    medicineId = r.getInt("ID"); storedName = r.getString("Name"); stock = r.getInt("Quantity"); price = r.getBigDecimal("Price");
                    if (r.next()) throw new IllegalArgumentException("Medicine names must be unique before recording a sale.");
                }
            }
            if (quantity > stock) throw new IllegalArgumentException("Only " + stock + " units are available.");
            try (PreparedStatement s = connection.prepareStatement("UPDATE inventory SET Quantity=? WHERE ID=?")) {
                s.setInt(1, stock - quantity); s.setInt(2, medicineId); s.executeUpdate();
            }
            Sale sale = new Sale(0, null, customerName, storedName, quantity, price,
                    price.multiply(java.math.BigDecimal.valueOf(quantity)));
            try (PreparedStatement s = connection.prepareStatement(
                    "INSERT INTO sales (sale_date, customer_name, medicine_name, quantity, unit_price, total_amount) VALUES (NOW(), ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, sale.customerName()); s.setString(2, sale.medicineName()); s.setInt(3, sale.quantity());
                s.setBigDecimal(4, sale.unitPrice()); s.setBigDecimal(5, sale.total()); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    return keys.next() ? new Sale(keys.getInt(1), sale.date(), sale.customerName(), sale.medicineName(), sale.quantity(), sale.unitPrice(), sale.total()) : sale;
                }
            }
        });
    }

    @Override public void deleteAndRestock(int saleId) {
        database.transaction(connection -> {
            String medicineName; int quantity;
            try (PreparedStatement s = connection.prepareStatement("SELECT medicine_name, quantity FROM sales WHERE id=? FOR UPDATE")) {
                s.setInt(1, saleId);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("Sale was not found.");
                    medicineName = r.getString("medicine_name"); quantity = r.getInt("quantity");
                }
            }
            int medicineId;
            try (PreparedStatement s = connection.prepareStatement("SELECT ID FROM inventory WHERE Name=? FOR UPDATE")) {
                s.setString(1, medicineName);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new IllegalStateException("The sold medicine is missing; sale was not deleted.");
                    medicineId = r.getInt("ID");
                    if (r.next()) throw new IllegalStateException("Multiple inventory rows share this medicine name; sale was not deleted.");
                }
            }
            try (PreparedStatement s = connection.prepareStatement("UPDATE inventory SET Quantity=Quantity+? WHERE ID=?")) {
                s.setInt(1, quantity); s.setInt(2, medicineId);
                if (s.executeUpdate() != 1) throw new IllegalStateException("Inventory could not be restored; sale was not deleted.");
            }
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM sales WHERE id=?")) { s.setInt(1, saleId); s.executeUpdate(); }
            return null;
        });
    }
}
