package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * Cell DTO includes parent sector reference.
 */
@Data
@Builder
public class CellDto {
    private Long id;
    private String name;
    private String code;
    private Long sectorId;
}
