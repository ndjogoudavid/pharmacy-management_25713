package rw.auca.ac.pharmacy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.auca.ac.pharmacy.model.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(
        regexp = "^\\+?2507[2-9]\\d{7}$",
        message = "Phone must be a valid Rwandan number (e.g., +250788123456)"
    )
    private String phone;

    private Role role;

    @jakarta.validation.constraints.NotNull(message = "Location ID is required")
    private Long locationId;

    private String password;
}
