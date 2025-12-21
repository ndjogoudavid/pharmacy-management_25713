package rw.auca.ac.pharmacy.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryDto {
    private long totalPatients;
    private long totalPharmacists;
    private long totalMedicines;
    private long lowStockMedicines;
    private long totalPrescriptions;
    private long filledPrescriptions;
    private long pendingPrescriptions;
    private long totalBills;
    private long paidBills;
    private BigDecimal totalRevenue;
    private List<RecentBillDto> recentBills;
    private List<RecentPrescriptionDto> recentPrescriptions;
}
