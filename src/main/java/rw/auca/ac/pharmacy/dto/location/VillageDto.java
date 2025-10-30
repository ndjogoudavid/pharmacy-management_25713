package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * Village DTO includes parent cell reference.
 */
@Data
@Builder
public class VillageDto {
    private Long id;
    private String name;
    private String code;
    private Long cellId;
}
