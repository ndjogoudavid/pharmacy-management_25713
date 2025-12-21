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
import rw.auca.ac.pharmacy.dto.PersonDto;
import rw.auca.ac.pharmacy.dto.PersonRequest;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.service.PersonService;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PersonDto> create(@RequestBody @Valid PersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PersonDto>> getAll(
            @NonNull @PageableDefault(size = 10, sort = "name") Pageable pageable,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.findAll(pageable, id, name, email, phone, role, location, q));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<PersonDto> me() {
        return ResponseEntity.ok(service.getSelf());
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @PutMapping("/me")
    public ResponseEntity<PersonDto> updateSelf(@RequestBody @Valid PersonRequest request) {
        return ResponseEntity.ok(service.updateSelf(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PersonDto> update(@PathVariable Long id, @RequestBody @Valid PersonRequest request) {
        return ResponseEntity.ok(service.updateByAdmin(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @GetMapping("/by-province")
    public ResponseEntity<Page<PersonDto>> getByProvinceName(@RequestParam String provinceName,
            @NonNull @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByProvinceName(provinceName, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @GetMapping("/search")
    public ResponseEntity<Page<PersonDto>> search(@NonNull @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) rw.auca.ac.pharmacy.model.enums.Role role,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.findAll(pageable, null, name, null, null, role, null, q));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @GetMapping("/by-province-code")
    public ResponseEntity<Page<PersonDto>> getByProvinceCode(@RequestParam String code,
            @NonNull @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByProvinceCode(code, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/{id}/province")
    public ResponseEntity<String> getPersonProvince(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPersonProvince(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/{id}/location-path")
    public ResponseEntity<rw.auca.ac.pharmacy.dto.location.LocationPathDto> getPersonLocationPath(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getPersonLocationPath(id));
    }
}
