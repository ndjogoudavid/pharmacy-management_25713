package rw.auca.ac.pharmacy.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Sale(int id, LocalDateTime date, String customerName, String medicineName,
                   int quantity, BigDecimal unitPrice, BigDecimal total) {
    public Sale {
        date = date == null ? LocalDateTime.now() : date;
        customerName = Medicine.required(customerName, "Customer name");
        medicineName = Medicine.required(medicineName, "Medicine name");
        if (quantity <= 0) throw new IllegalArgumentException("Sale quantity must be positive.");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
        unitPrice = unitPrice.setScale(2, java.math.RoundingMode.HALF_UP);
        total = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
