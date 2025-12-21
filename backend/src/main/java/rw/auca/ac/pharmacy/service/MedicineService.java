package rw.auca.ac.pharmacy.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private static class MedicineSpecifications {
        private static List<String> normalizedCategoryTokens(String value) {
            String trimmed = value.trim().toLowerCase();
            if (trimmed.isEmpty()) {
                return List.of();
            }
            String singular = trimmed.endsWith("s") && trimmed.length() > 1
                    ? trimmed.substring(0, trimmed.length() - 1)
                    : trimmed;
            String plural = trimmed.endsWith("s") ? trimmed : trimmed + "s";
            return List.of(trimmed, singular, plural).stream().distinct().toList();
        }

        public static Specification<Medicine> hasName(String name) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        }

        public static Specification<Medicine> hasSku(String sku) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("sku")), "%" + sku.toLowerCase() + "%");
        }

        public static Specification<Medicine> hasManufacturer(String manufacturer) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("manufacturer")),
                    "%" + manufacturer.toLowerCase() + "%");
        }

        public static Specification<Medicine> hasType(String type) {
            return (root, query, cb) -> {
                query.distinct(true);
                List<String> tokens = normalizedCategoryTokens(type);
                var categoryJoin = root.join("categories", jakarta.persistence.criteria.JoinType.LEFT);
                List<Predicate> predicates = new ArrayList<>();
                for (String token : tokens) {
                    predicates.add(cb.equal(cb.lower(root.get("type")), token));
                    predicates.add(cb.equal(cb.lower(categoryJoin.get("name")), token));
                }
                return cb.or(predicates.toArray(new Predicate[0]));
            };
        }

        public static Specification<Medicine> hasUnit(String unit) {
            return (root, query, cb) -> cb.equal(cb.lower(root.get("unit")), unit.toLowerCase());
        }

        public static Specification<Medicine> isControlled(boolean controlled) {
            return (root, query, cb) -> cb.equal(root.get("isControlled"), controlled);
        }

        public static Specification<Medicine> matchesQuery(String term) {
            return (root, query, cb) -> {
                query.distinct(true);
                String like = "%" + term.toLowerCase() + "%";
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("name")), like));
                predicates.add(cb.like(cb.lower(root.get("sku")), like));
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("manufacturer"), "")), like));
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("type"), "")), like));
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("unit"), "")), like));
                var categoryJoin = root.join("categories", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), like));
                List<String> tokens = normalizedCategoryTokens(term);
                for (String token : tokens) {
                    String tokenLike = "%" + token + "%";
                    predicates.add(cb.like(cb.lower(cb.coalesce(root.get("type"), "")), tokenLike));
                    predicates.add(cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), tokenLike));
                }

                String normalized = term.trim().toLowerCase();
                if (normalized.equals("controlled") || normalized.equals("true") || normalized.equals("yes")) {
                    predicates.add(cb.isTrue(root.get("isControlled")));
                }
                if (normalized.equals("regular") || normalized.equals("false") || normalized.equals("no")) {
                    predicates.add(cb.isFalse(root.get("isControlled")));
                }

                try {
                    Long id = Long.parseLong(term);
                    predicates.add(cb.equal(root.get("id"), id));
                    predicates.add(cb.equal(root.get("quantity"), id.intValue()));
                    predicates.add(cb.equal(root.get("reorderLevel"), id.intValue()));
                } catch (NumberFormatException ex) {
                    // ignore non-numeric
                }
                try {
                    BigDecimal amount = new BigDecimal(term);
                    predicates.add(cb.equal(root.get("price"), amount));
                } catch (NumberFormatException ex) {
                    // ignore non-decimal
                }

                return cb.or(predicates.toArray(new Predicate[0]));
            };
        }
    }

    public @NonNull Page<MedicineDto> findAll(@NonNull Pageable pageable, String name, String sku, String manufacturer,
            String type, String unit, String controlled, String q) {
        Specification<Medicine> spec = Specification.allOf();
        if (q != null && !q.isBlank()) {
            spec = spec.and(MedicineSpecifications.matchesQuery(q.trim()));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(MedicineSpecifications.hasName(name.trim()));
        }
        if (sku != null && !sku.isBlank()) {
            spec = spec.and(MedicineSpecifications.hasSku(sku.trim()));
        }
        if (manufacturer != null && !manufacturer.isBlank()) {
            spec = spec.and(MedicineSpecifications.hasManufacturer(manufacturer.trim()));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and(MedicineSpecifications.hasType(type.trim()));
        }
        if (unit != null && !unit.isBlank()) {
            spec = spec.and(MedicineSpecifications.hasUnit(unit.trim()));
        }
        if (controlled != null && !controlled.isBlank()) {
            spec = spec.and(MedicineSpecifications.isControlled(Boolean.parseBoolean(controlled)));
        }
        return repository.findAll(spec, pageable).map(this::toDto);
    }

    public MedicineDto create(MedicineRequest request) {
        validateRequest(request);

        Medicine medicine = Objects.requireNonNull(Medicine.builder()
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
                .build());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = request.getCategoryIds().stream()
                    .map(categoryRepo::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            medicine.getCategories().addAll(categories);
        }

        Medicine saved = repository.save(medicine);
        log.info("Created new medicine: {}", saved.getName());
        return toDto(saved);
    }

    public MedicineDto get(@NonNull Long id) {
        Medicine medicine = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id)));
        return toDto(medicine);
    }

    public List<MedicineDto> search(String name) {
        return repository.findByNameContainingIgnoreCaseOrderByNameAsc(name).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public MedicineDto update(@NonNull Long id, MedicineRequest request) {
        validateRequest(request);

        Medicine medicine = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id)));

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
        log.info("Updated medicine ID {}: {}", id, updated.getName());
        return toDto(updated);
    }

    public void delete(@NonNull Long id) {
        Medicine medicine = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MEDICINE_NOT_FOUND_MSG + id)));
        medicine.setDeletedAt(LocalDateTime.now());
        repository.save(medicine);
        log.warn("Soft-deleted medicine ID {}", id);
    }

    public List<MedicineDto> getLowStock() {
        List<Medicine> low = repository.findLowStockByOwnThreshold();
        if (low.isEmpty()) {
            // fallback to default threshold so dashboards still show something even if
            // reorder levels are missing
            low = repository.findByQuantityLessThanOrderByNameAsc(DEFAULT_REORDER_THRESHOLD);
        }
        return low
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
                .categoryIds(medicine.getCategories() == null ? List.of()
                        : medicine.getCategories().stream()
                                .map(Category::getId)
                                .toList())
                .categories(medicine.getCategories() == null ? List.of()
                        : medicine.getCategories().stream()
                                .map(c -> rw.auca.ac.pharmacy.dto.CategoryDto.builder()
                                        .id(c.getId())
                                        .name(c.getName())
                                        .description(c.getDescription())
                                        .build())
                                .toList())
                .build();
    }
}
