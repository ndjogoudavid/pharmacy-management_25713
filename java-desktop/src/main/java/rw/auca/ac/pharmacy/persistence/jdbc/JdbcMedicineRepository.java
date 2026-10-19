package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.MedicineRepository;
import rw.auca.ac.pharmacy.domain.Medicine;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcMedicineRepository implements MedicineRepository {
    private final Database database;
    public JdbcMedicineRepository(Database database) { this.database = database; }

    @Override public List<Medicine> findAll() {
        return database.query(connection -> {
            List<Medicine> rows = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT ID, Name, Quantity, Price, Category FROM inventory ORDER BY ID");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) rows.add(map(result));
            }
            return rows;
        });
    }

    @Override public Optional<Medicine> findByName(String name) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ID, Name, Quantity, Price, Category FROM inventory WHERE Name = ? ORDER BY ID LIMIT 1")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(map(result)) : Optional.empty(); }
            }
        });
    }

    @Override public Medicine save(Medicine medicine) {
        return database.transaction(connection -> {
            try (PreparedStatement duplicate = connection.prepareStatement(
                    "SELECT COUNT(*) FROM inventory WHERE LOWER(Name)=LOWER(?) AND ID<>?")) {
                duplicate.setString(1, medicine.name()); duplicate.setInt(2, medicine.id());
                try (ResultSet result = duplicate.executeQuery()) {
                    result.next();
                    if (result.getLong(1) > 0) throw new IllegalArgumentException("A medicine with this name already exists.");
                }
            }
            if (medicine.id() > 0) {
                String previousName;
                try (PreparedStatement lookup = connection.prepareStatement("SELECT Name FROM inventory WHERE ID=? FOR UPDATE")) {
                    lookup.setInt(1, medicine.id());
                    try (ResultSet result = lookup.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Medicine was not found.");
                        previousName = result.getString("Name");
                    }
                }
                if (!previousName.equalsIgnoreCase(medicine.name())) {
                    try (PreparedStatement usage = connection.prepareStatement("SELECT COUNT(*) FROM sales WHERE medicine_name=?")) {
                        usage.setString(1, previousName);
                        try (ResultSet result = usage.executeQuery()) {
                            result.next();
                            if (result.getLong(1) > 0) throw new IllegalArgumentException("A medicine with sales history cannot be renamed.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE inventory SET Name=?, Quantity=?, Price=?, Category=? WHERE ID=?")) {
                    bind(statement, medicine); statement.setInt(5, medicine.id());
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("Medicine update did not affect one row.");
                }
                return medicine;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO inventory (Name, Quantity, Price, Category) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, medicine); statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? new Medicine(keys.getInt(1), medicine.name(), medicine.quantity(), medicine.price(), medicine.category()) : medicine;
                }
            }
        });
    }

    private static void bind(PreparedStatement statement, Medicine medicine) throws SQLException {
        statement.setString(1, medicine.name()); statement.setInt(2, medicine.quantity());
        statement.setBigDecimal(3, medicine.price()); statement.setString(4, medicine.category());
    }
    private static Medicine map(ResultSet result) throws SQLException {
        return new Medicine(result.getInt("ID"), result.getString("Name"), result.getInt("Quantity"),
                result.getBigDecimal("Price"), result.getString("Category"));
    }
    @Override public void deleteById(int id) {
        database.transaction(connection -> {
            String name;
            try (PreparedStatement s = connection.prepareStatement("SELECT Name FROM inventory WHERE ID=? FOR UPDATE")) {
                s.setInt(1, id);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("Medicine was not found.");
                    name = r.getString("Name");
                }
            }
            try (PreparedStatement s = connection.prepareStatement("SELECT COUNT(*) FROM sales WHERE medicine_name=?")) {
                s.setString(1, name);
                try (ResultSet r = s.executeQuery()) {
                    r.next();
                    if (r.getLong(1) > 0) throw new IllegalArgumentException("A medicine with sales history cannot be deleted.");
                }
            }
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM inventory WHERE ID=?")) { s.setInt(1, id); s.executeUpdate(); }
            return null;
        });
    }
}
