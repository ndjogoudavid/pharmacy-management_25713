package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;
import rw.auca.ac.pharmacy.model.enums.LocationType;

@Data
@Builder
public class LocationDto {
    private Long id;
    private String code;
    private String name;
    private LocationType type;
    private Long parentId;
}
