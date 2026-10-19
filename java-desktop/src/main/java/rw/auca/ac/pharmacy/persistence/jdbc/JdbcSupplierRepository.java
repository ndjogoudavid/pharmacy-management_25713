package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.SupplierRepository;
import rw.auca.ac.pharmacy.domain.Supplier;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcSupplierRepository implements SupplierRepository {
    private final Database database;
    public JdbcSupplierRepository(Database database) { this.database = database; }
    @Override public List<Supplier> findAll() {
        return database.query(connection -> {
            List<Supplier> rows = new ArrayList<>();
            try (PreparedStatement s = connection.prepareStatement("SELECT ID, Company_Name, Contact_Person, Email, Address, Products FROM suppliers ORDER BY ID"); ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(map(r));
            }
            return rows;
        });
    }
    @Override public Supplier save(Supplier supplier) {
        return database.query(connection -> {
            String sql = supplier.id() > 0
                    ? "UPDATE suppliers SET Company_Name=?, Contact_Person=?, Email=?, Address=?, Products=? WHERE ID=?"
                    : "INSERT INTO suppliers (Company_Name, Contact_Person, Email, Address, Products) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement s = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(s, supplier);
                if (supplier.id() > 0) s.setInt(6, supplier.id());
                s.executeUpdate();
                if (supplier.id() <= 0) try (ResultSet keys = s.getGeneratedKeys()) {
                    if (keys.next()) return new Supplier(keys.getInt(1), supplier.companyName(), supplier.contactPerson(), supplier.email(), supplier.address(), supplier.products());
                }
            }
            return supplier;
        });
    }
    private static void bind(PreparedStatement s, Supplier v) throws SQLException {
        s.setString(1, v.companyName()); s.setString(2, v.contactPerson()); s.setString(3, v.email()); s.setString(4, v.address()); s.setString(5, v.products());
    }
    private static Supplier map(ResultSet r) throws SQLException {
        return new Supplier(r.getInt("ID"), r.getString("Company_Name"), r.getString("Contact_Person"), r.getString("Email"), r.getString("Address"), r.getString("Products"));
    }
    @Override public void deleteById(int id) {
        database.query(connection -> { try (PreparedStatement s = connection.prepareStatement("DELETE FROM suppliers WHERE ID=?")) { s.setInt(1, id); s.executeUpdate(); } return null; });
    }
}
