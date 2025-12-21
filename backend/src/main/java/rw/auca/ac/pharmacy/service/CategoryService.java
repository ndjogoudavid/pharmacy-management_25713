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
import rw.auca.ac.pharmacy.dto.CategoryDto;
import rw.auca.ac.pharmacy.dto.CategoryRequest;
import rw.auca.ac.pharmacy.model.core.Category;
import rw.auca.ac.pharmacy.repository.CategoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {
    private static final String CATEGORY_NOT_FOUND_MSG = "Category not found with ID: ";

    private final CategoryRepository repository;

    private static class CategorySpecifications {
        public static Specification<Category> hasId(Long id) {
            return (root, query, cb) -> cb.equal(root.get("id"), id);
        }

        public static Specification<Category> hasName(String name) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        }

        public static Specification<Category> hasDescription(String description) {
            return (root, query, cb) -> cb.like(cb.lower(cb.coalesce(root.get("description"), "")),
                    "%" + description.toLowerCase() + "%");
        }

        public static Specification<Category> matchesQuery(String term) {
            return (root, query, cb) -> {
                String like = "%" + term.toLowerCase() + "%";
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("name")), like));
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like));
                try {
                    Long id = Long.parseLong(term);
                    predicates.add(cb.equal(root.get("id"), id));
                } catch (NumberFormatException ex) {
                    // ignore non-numeric
                }
                return cb.or(predicates.toArray(new Predicate[0]));
            };
        }
    }

    public @NonNull Page<CategoryDto> findAll(@NonNull Pageable pageable, String id, String name, String description,
            String q) {
        String nameTerm = (name != null && !name.isBlank()) ? name.trim() : null;
        String descTerm = (description != null && !description.isBlank()) ? description.trim() : null;
        String qTerm = (q != null && !q.isBlank()) ? q.trim() : null;

        if ((id == null || id.isBlank()) && nameTerm == null && descTerm == null && qTerm == null) {
            return repository.findAll(pageable).map(this::toDto);
        }

        Specification<Category> spec = Specification.allOf();
        if (id != null && !id.isBlank()) {
            String idTerm = id.trim();
            spec = spec.and((root, query, cb) -> cb.like(cb.toString(root.get("id")), idTerm + "%"));
        }
        if (qTerm != null) {
            spec = spec.and(CategorySpecifications.matchesQuery(qTerm));
        }
        if (nameTerm != null) {
            spec = spec.and(CategorySpecifications.hasName(nameTerm));
        }
        if (descTerm != null) {
            spec = spec.and(CategorySpecifications.hasDescription(descTerm));
        }

        return repository.findAll(spec, pageable).map(this::toDto);
    }

    public CategoryDto create(CategoryRequest request) {
        validateRequest(request);
        Category category = Objects.requireNonNull(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
        Category saved = repository.save(category);
        log.info("Created new category: {}", saved.getName());
        return toDto(saved);
    }

    public CategoryDto get(@NonNull Long id) {
        Category category = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id)));
        return toDto(category);
    }

    public CategoryDto update(@NonNull Long id, CategoryRequest request) {
        validateRequest(request);
        Category category = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id)));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category updated = repository.save(category);
        log.info("Updated category ID {}: {}", id, updated.getName());
        return toDto(updated);
    }

    public void delete(@NonNull Long id) {
        Category category = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id)));
        category.setDeletedAt(LocalDateTime.now());
        repository.save(category);
        log.warn("Soft-deleted category ID {}", id);
    }

    private void validateRequest(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
