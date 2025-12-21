package rw.auca.ac.pharmacy.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String status;
    private String token;
    private String role;
    private String name;
    private String email;
}
