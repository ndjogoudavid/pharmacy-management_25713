package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.MedicineDto;
import rw.auca.ac.pharmacy.dto.MedicineRequest;
import rw.auca.ac.pharmacy.model.core.Category;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.repository.CategoryRepository;
import rw.auca.ac.pharmacy.repository.MedicineRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MedicineService {

    private static final String MEDICINE_NOT_FOUND_MSG = "Medicine not found with ID: ";
    private static final int DEFAULT_REORDER_THRESHOLD = 10;

    private final MedicineRepository repository;
    private final CategoryRepository categoryRepo;

    public @NonNull Page<MedicineDto> findAll(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public MedicineDto create(MedicineRequest request) {
        validateRequest(request);

        Medicine medicine = Medicine.builder()
                .sku(request.getSku())
                .name(request.getName())
                .type(request.getType())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .reorderLevel(request.getReorderLevel())
                .price(request.getPrice())
                .manufacturer(request.getManufacturer())
                .isControlled(request.getIsControlled())
                .expiryDate(request.getExpiryDate())
                .build();

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = request.getCategoryIds().stream()
                    .map(categoryRepo::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            medicine.getCategories().addAll(categories);
        }

        Medicine saved = repository.save(medicine);
        log.info("✅ Created new medicine: {}", saved.getName());
        return toDto(saved);
    }

    public MedicineDto get(Long id) {
        Medicine medicine = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id));
        return toDto(medicine);
    }

    public List<MedicineDto> search(String name) {
        return repository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public MedicineDto update(Long id, MedicineRequest request) {
        validateRequest(request);

        Medicine medicine = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id));

        medicine.setSku(request.getSku());
        medicine.setName(request.getName());
        medicine.setType(request.getType());
        medicine.setUnit(request.getUnit());
        medicine.setQuantity(request.getQuantity());
        medicine.setReorderLevel(request.getReorderLevel());
        medicine.setPrice(request.getPrice());
        medicine.setManufacturer(request.getManufacturer());
        medicine.setIsControlled(request.getIsControlled());
        medicine.setExpiryDate(request.getExpiryDate());

        medicine.getCategories().clear();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = request.getCategoryIds().stream()
                    .map(categoryRepo::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            medicine.getCategories().addAll(categories);
        }

        Medicine updated = repository.save(medicine);
        log.info("✅ Updated medicine ID {}: {}", id, updated.getName());
        return toDto(updated);
    }

    public void delete(Long id) {
        Medicine medicine = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id));

        medicine.setDeletedAt(LocalDateTime.now());
        repository.save(medicine);
        log.warn("🗑️ Soft-deleted medicine ID {}", id);
    }

    /**
     * Returns all medicines whose quantity is below the reorder threshold.
     * The threshold is configurable through DEFAULT_REORDER_THRESHOLD.
     */
    public List<MedicineDto> getLowStock() {
        return repository.findByQuantityLessThan(DEFAULT_REORDER_THRESHOLD)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validateRequest(MedicineRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Medicine request cannot be null");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    private MedicineDto toDto(Medicine medicine) {
        return MedicineDto.builder()
                .id(medicine.getId())
                .sku(medicine.getSku())
                .name(medicine.getName())
                .type(medicine.getType())
                .unit(medicine.getUnit())
                .quantity(medicine.getQuantity())
                .reorderLevel(medicine.getReorderLevel())
                .price(medicine.getPrice())
                .manufacturer(medicine.getManufacturer())
                .isControlled(medicine.getIsControlled())
                .expiryDate(medicine.getExpiryDate())
                .build();
    }
}
