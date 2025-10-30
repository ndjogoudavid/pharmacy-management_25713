package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.auca.ac.pharmacy.dto.MedicineDto;
import rw.auca.ac.pharmacy.dto.MedicineRequest;
import rw.auca.ac.pharmacy.service.MedicineService;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService service;

    @GetMapping
    public ResponseEntity<Page<MedicineDto>> getAll(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineDto create(@RequestBody @Valid MedicineRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public MedicineDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public MedicineDto update(@PathVariable Long id, @RequestBody @Valid MedicineRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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