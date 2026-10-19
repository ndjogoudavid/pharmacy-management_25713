package rw.auca.ac.pharmacy.persistence.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

public final class Database {
    @FunctionalInterface public interface SqlWork<T> { T run(Connection connection) throws SQLException; }
    private final String url;
    private final String username;
    private final String password;

    public Database() {
        url = env("PHARMACY_DB_URL", "jdbc:postgresql://localhost:5432/inventory_management");
        username = env("PHARMACY_DB_USER", "postgres");
        password = System.getenv("PHARMACY_DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException(
                "Set PHARMACY_DB_PASSWORD before starting the pharmacy application.");
    }

    public Connection open() throws SQLException { return DriverManager.getConnection(url, username, password); }
    public <T> T query(SqlWork<T> work) {
        try (Connection connection = open()) { return work.run(connection); }
        catch (SQLException ex) { throw new DataAccessException("Database operation failed.", ex); }
    }

    public <T> T transaction(SqlWork<T> work) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                if (ex instanceof SQLException sql) throw new DataAccessException("Database transaction failed.", sql);
                throw ex;
            }
        } catch (SQLException ex) { throw new DataAccessException("Could not open a database transaction.", ex); }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
