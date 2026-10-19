package rw.auca.ac.pharmacy.domain;

import java.math.BigDecimal;

public record Medicine(int id, String name, int quantity, BigDecimal price, String category) {
    public Medicine {
        name = required(name, "Medicine name");
        category = required(category, "Category");
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Price cannot be negative.");
        price = price.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
}
