package rw.auca.ac.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionUpdateRequest {

    private Long patientId;
    private Long pharmacistId;
    private LocalDateTime dateIssued;
    private String notes;
    private String walkInName;
    private String walkInPhone;
    private PrescriptionStatus status;
    @Valid
    private List<ItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        @NotNull(message = "Medicine ID is required")
        private Long medicineId;
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }
}
