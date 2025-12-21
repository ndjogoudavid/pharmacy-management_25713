package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.PageRequest;
import rw.auca.ac.pharmacy.dto.PrescriptionCreateRequest;
import rw.auca.ac.pharmacy.dto.PrescriptionDto;
import rw.auca.ac.pharmacy.dto.PrescriptionUpdateRequest;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.service.PrescriptionService;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService service;

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @PostMapping
    public ResponseEntity<PrescriptionDto> create(@RequestBody @Valid PrescriptionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    // Get one by ID
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // Get all prescriptions paginated
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping
    public ResponseEntity<Page<PrescriptionDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) PrescriptionStatus status,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String pharmacistName,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "id,desc") String sort) {
        // Parse sort parameter (e.g. "id,desc" -> Sort.by("id").descending())
        String[] parts = sort.split(",");
        String sortField = parts[0];
        org.springframework.data.domain.Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, sortField));
        return ResponseEntity.ok(service.getAll(pageable, id, status, patientName, pharmacistName, q));
    }

    // Update existing prescription
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionDto> update(
            @PathVariable Long id,
            @RequestBody @Valid PrescriptionUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // Explicit fill action for prescriptions
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @PostMapping("/{id}/fill")
    public ResponseEntity<PrescriptionDto> fill(
            @PathVariable Long id,
            @RequestBody @Valid rw.auca.ac.pharmacy.dto.PrescriptionFillRequest request) {
        return ResponseEntity.ok(service.fill(id, request));
    }

    // Soft delete
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
