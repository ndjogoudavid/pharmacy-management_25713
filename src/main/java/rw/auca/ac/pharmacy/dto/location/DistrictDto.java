package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * District DTO includes a link back to its parent Province.
 */
@Data
@Builder
public class DistrictDto {
    private Long id;
    private String name;
    private String code;
    private Long provinceId;
}
