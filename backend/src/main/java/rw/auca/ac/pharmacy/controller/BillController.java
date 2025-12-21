package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import rw.auca.ac.pharmacy.dto.BillDto;
import rw.auca.ac.pharmacy.dto.BillRequest;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;
import rw.auca.ac.pharmacy.service.BillService;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService service;

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<BillDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping("/by-prescription/{prescriptionId}")
    public ResponseEntity<BillDto> getByPrescription(@PathVariable Long prescriptionId) {
        return ResponseEntity.ok(service.getByPrescription(prescriptionId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','PATIENT')")
    @GetMapping
    public ResponseEntity<Page<BillDto>> getAll(Pageable pageable,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String prescriptionId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String paymentRef,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(
                service.getAll(pageable, id, prescriptionId, status, paymentMethod, paymentRef, patientName, q));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    @PutMapping("/{id}")
    public ResponseEntity<BillDto> update(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
