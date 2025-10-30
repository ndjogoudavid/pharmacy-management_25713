package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * Sector DTO includes parent district reference.
 */
@Data
@Builder
public class SectorDto {
    private Long id;
    private String name;
    private String code;
    private Long districtId;
}
