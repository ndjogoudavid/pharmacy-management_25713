package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.PrescriptionItemDto;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.repository.PrescriptionItemRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrescriptionItemService {
    private static final String ITEM_NOT_FOUND_MSG = "PrescriptionItem not found with ID: ";

    private final PrescriptionItemRepository repository;

    public PrescriptionItemDto get(Long id) {
        PrescriptionItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ITEM_NOT_FOUND_MSG + id));
        return toDto(item);
    }

    public void delete(Long id) {
        PrescriptionItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ITEM_NOT_FOUND_MSG + id));
        repository.delete(item);
        log.warn("🗑️ Deleted prescription item ID {}", id);
    }

    private PrescriptionItemDto toDto(PrescriptionItem item) {
        return PrescriptionItemDto.builder()  // Fixed: @Builder now works
                .id(item.getId())
                .prescriptionId(item.getPrescription().getId())
                .medicineId(item.getMedicine().getId())
                .quantity(item.getQuantity())
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .duration(item.getDuration())
                .instructions(item.getInstructions())
                .build();
    }
}