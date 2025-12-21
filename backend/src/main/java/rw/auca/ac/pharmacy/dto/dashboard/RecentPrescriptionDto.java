package rw.auca.ac.pharmacy.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentPrescriptionDto {
    private Long id;
    private String patientName;
    private String walkInName;
    private String walkInPhone;
    private String status;
    private LocalDateTime dateIssued;
}
