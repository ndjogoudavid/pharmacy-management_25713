package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import rw.auca.ac.pharmacy.dto.MedicineDto;
import rw.auca.ac.pharmacy.dto.MedicineRequest;
import rw.auca.ac.pharmacy.service.MedicineService;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
public class MedicineController {

    private final MedicineService service;

    @GetMapping
    public ResponseEntity<Page<MedicineDto>> getAll(
            @NonNull @PageableDefault(size = 10, sort = "name") Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String controlled,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.findAll(pageable, name, sku, manufacturer, type, unit, controlled, q));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineDto create(@RequestBody @Valid MedicineRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public MedicineDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineDto update(@PathVariable Long id, @RequestBody @Valid MedicineRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineDto>> search(@RequestParam String name) {
        return ResponseEntity.ok(service.search(name));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<MedicineDto>> getLowStock() {
        return ResponseEntity.ok(service.getLowStock());
    }
}
