package rw.auca.ac.pharmacy.model.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "prescription_item",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_prescription_medicine",
        columnNames = {"prescription_id", "medicine_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity;

    private Integer dispensedQuantity;

    @Column(length = 50)
    private String dosage;

    @Column(length = 50)
    private String frequency;

    @Column(length = 50)
    private String duration;

    @Column(columnDefinition = "text")
    private String instructions;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean dispensed = false;

    private LocalDateTime dispensedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
