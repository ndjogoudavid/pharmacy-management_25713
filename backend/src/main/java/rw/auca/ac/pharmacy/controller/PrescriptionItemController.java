package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import rw.auca.ac.pharmacy.dto.PrescriptionItemDto;
import rw.auca.ac.pharmacy.service.PrescriptionItemService;

@RestController
@RequestMapping("/api/prescription-items")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
public class PrescriptionItemController {

    private final PrescriptionItemService service;

    // CREATE
    @PostMapping
    public ResponseEntity<PrescriptionItemDto> create(@Valid @RequestBody PrescriptionItemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionItemDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // READ ALL (paged)
    @GetMapping
    public ResponseEntity<Page<PrescriptionItemDto>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionItemDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionItemDto dto
    ) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
