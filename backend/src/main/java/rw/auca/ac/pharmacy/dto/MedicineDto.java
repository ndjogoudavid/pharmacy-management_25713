package rw.auca.ac.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDto {
    private Long id;
    private String sku;
    private String name;
    private String type;
    private String unit;
    private Integer quantity;
    private Integer reorderLevel;
    private BigDecimal price;
    private String manufacturer;
    private Boolean isControlled;
    private LocalDate expiryDate;
    private List<Long> categoryIds;
    private List<CategoryDto> categories;
}
