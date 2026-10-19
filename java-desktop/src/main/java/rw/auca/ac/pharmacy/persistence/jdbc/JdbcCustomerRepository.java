package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.CustomerRepository;
import rw.auca.ac.pharmacy.domain.Customer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcCustomerRepository implements CustomerRepository {
    private final Database database;
    public JdbcCustomerRepository(Database database) { this.database = database; }
    @Override public List<Customer> findAll() {
        return database.query(connection -> {
            List<Customer> rows = new ArrayList<>();
            try (PreparedStatement s = connection.prepareStatement("SELECT Name, Phone, Email, Address FROM customers ORDER BY Name"); ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new Customer(r.getString("Name"), r.getString("Phone"), r.getString("Email"), r.getString("Address")));
            }
            return rows;
        });
    }
    @Override public void save(Customer customer, String originalName) {
        database.query(connection -> {
            boolean update = originalName != null && !originalName.isBlank();
            String sql = update ? "UPDATE customers SET Name=?, Phone=?, Email=?, Address=? WHERE Name=?"
                    : "INSERT INTO customers (Name, Phone, Email, Address) VALUES (?, ?, ?, ?)";
            try (PreparedStatement s = connection.prepareStatement(sql)) {
                s.setString(1, customer.name()); s.setString(2, customer.phone()); s.setString(3, customer.email()); s.setString(4, customer.address());
                if (update) s.setString(5, originalName);
                s.executeUpdate();
            }
            return null;
        });
    }
    @Override public void deleteByName(String name) {
        database.query(connection -> { try (PreparedStatement s = connection.prepareStatement("DELETE FROM customers WHERE Name=?")) { s.setString(1, name); s.executeUpdate(); } return null; });
    }
}
