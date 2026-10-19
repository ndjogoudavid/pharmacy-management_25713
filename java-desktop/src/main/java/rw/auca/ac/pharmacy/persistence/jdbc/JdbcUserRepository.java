package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.UserRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    private final Database database;
    public JdbcUserRepository(Database database) { this.database = database; }
    @Override public Optional<String> findPassword(String username) {
        return database.query(connection -> {
            try (PreparedStatement s = connection.prepareStatement("SELECT password FROM users WHERE username=?")) {
                s.setString(1, username);
                try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.ofNullable(r.getString("password")) : Optional.empty(); }
            }
        });
    }
    @Override public void updatePassword(String username, String encodedPassword) {
        database.query(connection -> {
            try (PreparedStatement s = connection.prepareStatement("UPDATE users SET password=? WHERE username=?")) {
                s.setString(1, encodedPassword); s.setString(2, username); s.executeUpdate();
            }
            return null;
        });
    }
}
