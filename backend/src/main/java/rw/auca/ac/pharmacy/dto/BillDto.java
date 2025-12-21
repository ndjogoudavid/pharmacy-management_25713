package rw.auca.ac.pharmacy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class BillDto {
    private Long id;
    private Long prescriptionId;
    private Long patientId;
    private String patientName;
    private String customerName;
    private String customerPhone;
    private String status;
    private String paymentMethod;
    private String paymentRef;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal netTotal;
    private String currency;

    @NotNull(message = "Payment date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentDate;
}
