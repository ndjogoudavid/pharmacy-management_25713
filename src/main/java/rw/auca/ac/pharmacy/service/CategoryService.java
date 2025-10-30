package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.CategoryDto;
import rw.auca.ac.pharmacy.dto.CategoryRequest;
import rw.auca.ac.pharmacy.model.core.Category;
import rw.auca.ac.pharmacy.repository.CategoryRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {
    private static final String CATEGORY_NOT_FOUND_MSG = "Category not found with ID: ";

    private final CategoryRepository repository;

    public @NonNull Page<CategoryDto> findAll(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public CategoryDto create(CategoryRequest request) {
        validateRequest(request);
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Category saved = repository.save(category);
        log.info("✅ Created new category: {}", saved.getName());
        return toDto(saved);
    }

    public CategoryDto get(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id));
        return toDto(category);
    }

    public CategoryDto update(Long id, CategoryRequest request) {
        validateRequest(request);
        Category category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category updated = repository.save(category);
        log.info("✅ Updated category ID {}: {}", id, updated.getName());
        return toDto(updated);
    }

    public void delete(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_MSG + id));
        category.setDeletedAt(LocalDateTime.now());  // Fixed: LocalDateTime
        repository.save(category);
        log.warn("🗑️ Soft-deleted category ID {}", id);
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