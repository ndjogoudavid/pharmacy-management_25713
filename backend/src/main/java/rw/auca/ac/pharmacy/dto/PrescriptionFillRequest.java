package rw.auca.ac.pharmacy.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrescriptionFillRequest {
    private List<ItemToFill> items;

    @Data
    public static class ItemToFill {
        private Long prescriptionItemId;
        private Integer quantity;
    }
}
