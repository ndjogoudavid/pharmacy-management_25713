package rw.auca.ac.pharmacy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {
    private String status;
    private String paymentMethod;
    private String paymentRef;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be zero or positive")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", inclusive = true, message = "Tax must be zero or positive")
    private BigDecimal taxAmount;

    @NotNull(message = "Payment date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentDate;
}
