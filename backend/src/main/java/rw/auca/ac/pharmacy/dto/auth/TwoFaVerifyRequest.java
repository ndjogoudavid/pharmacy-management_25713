package rw.auca.ac.pharmacy.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFaVerifyRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}
