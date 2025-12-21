package rw.auca.ac.pharmacy.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentBillDto {
    private Long id;
    private Long prescriptionId;
    private BigDecimal netTotal;
    private LocalDateTime paymentDate;
    private String status;
}
