package rw.auca.ac.pharmacy.model.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"prescription"})
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", unique = true, nullable = false)
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String paymentRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Formula("(subtotal - discount_amount + tax_amount)")
    private BigDecimal netTotal;

    @Column(nullable = false, length = 5)
    private String currency;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}