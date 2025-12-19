package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.location.LocationDto;
import rw.auca.ac.pharmacy.dto.location.LocationRequest;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final LocationRepository repository;

    public LocationDto create(LocationRequest request) {
        validate(request, true);
        Location parent = resolveParent(request.getParentId());

        Location location = Location.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .type(request.getType())
                .parent(parent)
                .build();

        Location saved = repository.save(location);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public LocationDto get(Long id) {
        Location loc = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id));
        return toDto(loc);
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> list(Pageable pageable, LocationType type, Long parentId, String name) {
        String trimmedName = name != null && !name.isBlank() ? name.trim() : null;

        if (trimmedName != null && type != null && parentId != null) {
            return repository
                    .findByNameContainingIgnoreCaseAndParentIdAndType(trimmedName, parentId, type, pageable)
                    .map(this::toDto);
        }
        if (trimmedName != null && parentId != null) {
            return repository
                    .findByNameContainingIgnoreCaseAndParentId(trimmedName, parentId, pageable)
                    .map(this::toDto);
        }
        if (trimmedName != null && type != null) {
            return repository
                    .findByNameContainingIgnoreCaseAndType(trimmedName, type, pageable)
                    .map(this::toDto);
        }
        if (trimmedName != null) {
            return repository
                    .findByNameContainingIgnoreCase(trimmedName, pageable)
                    .map(this::toDto);
        }
        if (type != null && parentId != null) {
            return repository.findByParentIdAndType(parentId, type, pageable).map(this::toDto);
        }
        if (parentId != null) {
            return repository.findByParentId(parentId, pageable).map(this::toDto);
        }
        if (type != null) {
            return repository.findByType(type, pageable).map(this::toDto);
        }
        // No filters: honor Pageable sort (default from @PageableDefault is name ASC)
        return repository.findAll(pageable).map(this::toDto);
    }

    public LocationDto update(Long id, LocationRequest request) {
        validate(request, false);
        Location location = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id));

        Location parent = resolveParent(request.getParentId());
        location.setCode(request.getCode().trim());
        location.setName(request.getName().trim());
        location.setType(request.getType());
        location.setParent(parent);

        return toDto(repository.save(location));
    }

    public void delete(Long id) {
        Location location = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id));
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
