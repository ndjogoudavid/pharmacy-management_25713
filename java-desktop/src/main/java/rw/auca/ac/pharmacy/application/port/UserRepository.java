package rw.auca.ac.pharmacy.application.port;

import java.util.Optional;

public interface UserRepository {
    Optional<String> findPassword(String username);
    void updatePassword(String username, String encodedPassword);
}
