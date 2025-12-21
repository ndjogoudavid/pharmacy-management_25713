package rw.auca.ac.pharmacy.dto.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rw.auca.ac.pharmacy.model.enums.LocationType;

@Data
public class LocationRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type is required")
    private LocationType type;

    private Long parentId;
}
