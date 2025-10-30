package rw.auca.ac.pharmacy.dto;

import lombok.Data;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;

@Data
public class PrescriptionUpdateRequest {
    private PrescriptionStatus status;
    private String notes;
}