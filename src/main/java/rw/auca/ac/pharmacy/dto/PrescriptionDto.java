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
public class PrescriptionDto {
    private Long id;
    private LocalDate dateIssued;
    private String status;
    private Long personId;  // Fixed: Added for builder
    private Long pharmacistId;
    private String notes;
    private List<PrescriptionItemDto> items;
}