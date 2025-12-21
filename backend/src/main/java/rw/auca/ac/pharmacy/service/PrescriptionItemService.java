package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.PrescriptionItemDto;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionItemRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.util.Objects;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrescriptionItemService {
    private static final String ITEM_NOT_FOUND_MSG = "Prescription item not found with ID: ";

    private final PrescriptionItemRepository repository;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicineRepository medicineRepo;

    // CREATE
    public PrescriptionItemDto create(PrescriptionItemDto dto) {
        Long prescriptionId = dto.getPrescriptionId();
        if (prescriptionId == null) {
            throw new IllegalArgumentException("Prescription ID is required");
        }
        Long medicineId = dto.getMedicineId();
        if (medicineId == null) {
            throw new IllegalArgumentException("Medicine ID is required");
        }

        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
        Medicine medicine = medicineRepo.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        PrescriptionItem item = Objects.requireNonNull(PrescriptionItem.builder()
                .prescription(prescription)
                .medicine(medicine)
                .quantity(dto.getQuantity())
                .dosage(dto.getDosage())
                .frequency(dto.getFrequency())
                .duration(dto.getDuration())
                .instructions(dto.getInstructions())
                .build());

        item = repository.save(item);
        log.info("Created prescription item for prescription ID {}", dto.getPrescriptionId());
        return toDto(item);
    }

    // READ
    public PrescriptionItemDto get(@NonNull Long id) {
        PrescriptionItem item = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ITEM_NOT_FOUND_MSG + id)));
        return toDto(item);
    }

    // UPDATE
    public PrescriptionItemDto update(@NonNull Long id, PrescriptionItemDto dto) {
        PrescriptionItem item = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ITEM_NOT_FOUND_MSG + id)));

        if (dto.getQuantity() != null) item.setQuantity(dto.getQuantity());
        if (dto.getDosage() != null) item.setDosage(dto.getDosage());
        if (dto.getFrequency() != null) item.setFrequency(dto.getFrequency());
        if (dto.getDuration() != null) item.setDuration(dto.getDuration());
        if (dto.getInstructions() != null) item.setInstructions(dto.getInstructions());

        repository.save(item);
        log.info("Updated prescription item ID {}", id);
        return toDto(item);
    }

    // DELETE
    public void delete(@NonNull Long id) {
        PrescriptionItem item = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ITEM_NOT_FOUND_MSG + id)));
        repository.delete(item);
        log.warn("Deleted prescription item ID {}", id);
    }

    private PrescriptionItemDto toDto(PrescriptionItem item) {
        return PrescriptionItemDto.builder()
                .id(item.getId())
                .prescriptionId(item.getPrescription().getId())
                .medicineId(item.getMedicine().getId())
                .medicineName(item.getMedicine().getName())
                .quantity(item.getQuantity())
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .duration(item.getDuration())
                .instructions(item.getInstructions())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionItemDto> getAll(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }
}
