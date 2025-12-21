package rw.auca.ac.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Fixed: Added for .builder()
public class PrescriptionItemDto {
    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private String medicineName;
    private java.math.BigDecimal medicinePrice; // For receipt display
    private Integer quantity;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private boolean dispensed;
    private Integer dispensedQuantity;
}