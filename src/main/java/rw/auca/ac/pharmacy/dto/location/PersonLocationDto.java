package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregates all location levels for a person,
 * used when returning full address hierarchy.
 */
@Data
@Builder
public class PersonLocationDto {
    private ProvinceDto province;
    private DistrictDto district;
    private SectorDto sector;
    private CellDto cell;
    private VillageDto village; 
}
