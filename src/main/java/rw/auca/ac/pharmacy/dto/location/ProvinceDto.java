package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * Province DTO mirrors the Province entity but omits JPA relations.
 */
@Data
@Builder
public class ProvinceDto {
    private Long id;
    private String name;
    private String code;
}
