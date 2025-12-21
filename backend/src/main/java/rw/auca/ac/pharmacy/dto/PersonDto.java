package rw.auca.ac.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.auca.ac.pharmacy.model.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private Long locationId;
}
