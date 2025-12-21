package rw.auca.ac.pharmacy.dto.location;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationPathDto {
    private Long provinceId;
    private String provinceCode;
    private String provinceName;

    private Long districtId;
    private String districtCode;
    private String districtName;

    private Long sectorId;
    private String sectorCode;
    private String sectorName;

    private Long cellId;
    private String cellCode;
    private String cellName;

    private Long villageId;
    private String villageCode;
    private String villageName;
}
