package rw.auca.ac.pharmacy.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.location.LocationDto;
import rw.auca.ac.pharmacy.dto.location.LocationRequest;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final LocationRepository repository;

    public LocationDto create(LocationRequest request) {
        validate(request, true);
        Location parent = resolveParent(request.getParentId());

        Location location = Objects.requireNonNull(Location.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .type(request.getType())
                .parent(parent)
                .build());

        Location saved = repository.save(location);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public LocationDto get(@NonNull Long id) {
        Location loc = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id)));
        return toDto(loc);
    }

    private static class LocationSpecifications {
        public static Specification<Location> hasType(LocationType type) {
            return (root, query, cb) -> cb.equal(root.get("type"), type);
        }

        public static Specification<Location> hasParentId(Long parentId) {
            return (root, query, cb) -> cb.equal(root.get("parent").get("id"), parentId);
        }

        public static Specification<Location> nameContains(String term) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + term.toLowerCase() + "%");
        }

        public static Specification<Location> codeContains(String term) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + term.toLowerCase() + "%");
        }

        public static Specification<Location> matchesQuery(String term) {
            return (root, query, cb) -> {
                String like = "%" + term.toLowerCase() + "%";
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("name")), like));
                predicates.add(cb.like(cb.lower(root.get("code")), like));
                predicates.add(cb.like(cb.lower(root.get("type").as(String.class)), like));
                try {
                    Long id = Long.parseLong(term);
                    predicates.add(cb.equal(root.get("id"), id));
                    predicates.add(cb.equal(root.get("parent").get("id"), id));
                } catch (NumberFormatException ex) {
                    // ignore non-numeric
                }
                return cb.or(predicates.toArray(new Predicate[0]));
            };
        }
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> list(@NonNull Pageable pageable, LocationType type, Long parentId, String name,
            String code, String q) {
        String trimmedName = name != null && !name.isBlank() ? name.trim() : null;
        String trimmedCode = code != null && !code.isBlank() ? code.trim() : null;
        String qTerm = q != null && !q.isBlank() ? q.trim() : null;

        Specification<Location> spec = Specification.allOf();
        if (qTerm != null) {
            spec = spec.and(LocationSpecifications.matchesQuery(qTerm));
        }
        if (trimmedName != null) {
            spec = spec.and(LocationSpecifications.nameContains(trimmedName));
        }
        if (trimmedCode != null) {
            spec = spec.and(LocationSpecifications.codeContains(trimmedCode));
        }
        if (type != null) {
            spec = spec.and(LocationSpecifications.hasType(type));
        }
        if (parentId != null) {
            spec = spec.and(LocationSpecifications.hasParentId(parentId));
        }

        return repository.findAll(spec, pageable).map(this::toDto);
    }

    public LocationDto update(@NonNull Long id, LocationRequest request) {
        validate(request, false);
        Location location = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id)));

        Location parent = resolveParent(request.getParentId());
        location.setCode(request.getCode().trim());
        location.setName(request.getName().trim());
        location.setType(request.getType());
        location.setParent(parent);

        return toDto(repository.save(location));
    }

    public void delete(@NonNull Long id) {
        Location location = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id)));
        repository.delete(location);
    }

    private Location resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return repository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent location not found with ID: " + parentId));
    }

    private LocationDto toDto(Location loc) {
        Long parentId = Optional.ofNullable(loc.getParent()).map(Location::getId).orElse(null);
        return LocationDto.builder()
                .id(loc.getId())
                .code(loc.getCode())
                .name(loc.getName())
                .type(loc.getType())
                .parentId(parentId)
                .build();
    }

    private void validate(LocationRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Location request cannot be null");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        if (creating && repository.existsByCode(request.getCode().trim())) {
            throw new IllegalArgumentException("Location code already exists: " + request.getCode());
        }
    }
}
