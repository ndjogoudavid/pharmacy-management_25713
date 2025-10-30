package rw.auca.ac.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionCreateRequest {
    private Long patientId;
    private Long pharmacistId;
    private LocalDate dateIssued;
    private String notes;
    private List<ItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        private Long medicineId;
        private Integer quantity;
        private String dosage;  // Fixed: Added
        private String frequency;  // Fixed: Added
        private String duration;  // Fixed: Added
        private String instructions;  // Fixed: Added
    }
}