package rw.auca.ac.pharmacy.domain;

import java.math.BigDecimal;

public record PharmacyReport(long medicineCount, long lowStockCount, BigDecimal inventoryValue,
                             BigDecimal salesValue) {
    public PharmacyReport {
        inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
        salesValue = salesValue == null ? BigDecimal.ZERO : salesValue;
    }
}
