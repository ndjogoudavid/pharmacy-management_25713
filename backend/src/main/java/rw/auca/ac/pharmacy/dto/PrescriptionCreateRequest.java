package rw.auca.ac.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionCreateRequest {
    private Long patientId;
    @NotNull(message = "Pharmacist ID is required")
    private Long pharmacistId;
    private LocalDateTime dateIssued; // FIXED
    private String notes;
    private String walkInName;
    private String walkInPhone;
    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<ItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        @NotNull(message = "Medicine ID is required")
        private Long medicineId;
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }
}
