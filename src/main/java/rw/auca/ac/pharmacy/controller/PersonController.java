package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.auca.ac.pharmacy.dto.PersonDto;
import rw.auca.ac.pharmacy.dto.PersonRequest;
import rw.auca.ac.pharmacy.service.PersonService;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PersonDto> create(@RequestBody @Valid PersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<PersonDto>> getAll(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonDto> update(@PathVariable Long id, @RequestBody @Valid PersonRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/by-province")
    public ResponseEntity<Page<PersonDto>> getByProvinceName(@RequestParam String provinceName, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByProvinceName(provinceName, pageable));
    }

    @GetMapping("/by-province-code")
    public ResponseEntity<Page<PersonDto>> getByProvinceCode(@RequestParam String code, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByProvinceCode(code, pageable));
    }

    @GetMapping("/{id}/province")
    public ResponseEntity<String> getPersonProvince(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPersonProvince(id));
    }
}