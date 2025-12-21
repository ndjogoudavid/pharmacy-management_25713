package rw.auca.ac.pharmacy.dto;

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
public class PrescriptionDto {
    private Long id;
    private LocalDateTime dateIssued;  
    private String status;
    private Long personId;
    private String patientName;
    private Long pharmacistId;
    private String pharmacistName;
    private String notes;
    private String walkInName;
    private String walkInPhone;
    private List<PrescriptionItemDto> items;
}
