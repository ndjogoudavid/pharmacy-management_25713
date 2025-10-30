package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.PrescriptionCreateRequest;
import rw.auca.ac.pharmacy.dto.PrescriptionDto;
import rw.auca.ac.pharmacy.dto.PrescriptionItemDto;
import rw.auca.ac.pharmacy.dto.PrescriptionUpdateRequest;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.time.LocalDateTime;
import java.util.List;  // Fixed: Added import
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrescriptionService {

    private static final String PRESCRIPTION_NOT_FOUND_MSG = "Prescription not found with ID: ";

    private final PrescriptionRepository prescriptionRepo;
    private final PersonRepository personRepo;
    private final MedicineRepository medicineRepo;

    public PrescriptionDto create(PrescriptionCreateRequest request) {
        // Validate patient
        Person patient = personRepo.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        // Validate pharmacist
        Person pharmacist = personRepo.findById(request.getPharmacistId())
                .orElseThrow(() -> new IllegalArgumentException("Pharmacist not found"));

        if (pharmacist.getRole() != Role.PHARMACIST) {
            throw new IllegalArgumentException("Only pharmacists can issue prescriptions");
        }

        // Build prescription
        Prescription prescription = Prescription.builder()
                .dateIssued(request.getDateIssued())
                .status(PrescriptionStatus.PENDING)
                .person(patient)
                .pharmacist(pharmacist)
                .notes(request.getNotes())
                .items(List.of())  // Fixed: Now resolves with import
                .build();

        prescription = prescriptionRepo.save(prescription);

        // Process items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (PrescriptionCreateRequest.ItemRequest itemReq : request.getItems()) {
                Medicine medicine = medicineRepo.findById(itemReq.getMedicineId())
                        .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

                if (medicine.getQuantity() < itemReq.getQuantity()) {
                    throw new IllegalArgumentException(
                        "Insufficient stock for " + medicine.getName() +
                        ". Available: " + medicine.getQuantity() +
                        ", Requested: " + itemReq.getQuantity()
                    );
                }

                // Deduct stock
                medicine.setQuantity(medicine.getQuantity() - itemReq.getQuantity());
                medicineRepo.save(medicine);

                // Create item
                PrescriptionItem item = PrescriptionItem.builder()
                        .prescription(prescription)
                        .medicine(medicine)
                        .quantity(itemReq.getQuantity())
                        .dosage(itemReq.getDosage())
                        .frequency(itemReq.getFrequency())
                        .duration(itemReq.getDuration())
                        .instructions(itemReq.getInstructions())
                        .build();

                prescription.getItems().add(item);
            }
            prescriptionRepo.save(prescription);
        }

        log.info("✅ Created prescription ID {} for patient {}", prescription.getId(), patient.getName());
        return toDto(prescription);
    }

    public PrescriptionDto get(Long id) {
        Prescription prescription = prescriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND_MSG + id));
        return toDto(prescription);
    }

    public PrescriptionDto update(Long id, PrescriptionUpdateRequest request) {
        Prescription prescription = prescriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND_MSG + id));
        prescription.setStatus(request.getStatus());
        prescription.setNotes(request.getNotes());
        Prescription updated = prescriptionRepo.save(prescription);
        log.info("✅ Updated prescription ID {}", id);
        return toDto(updated);
    }

    public void delete(Long id) {
        Prescription prescription = prescriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PRESCRIPTION_NOT_FOUND_MSG + id));
        prescription.setDeletedAt(LocalDateTime.now());
        prescriptionRepo.save(prescription);
        log.warn("🗑️ Soft-deleted prescription ID {}", id);
    }

    private PrescriptionDto toDto(Prescription p) {
        return PrescriptionDto.builder()
                .id(p.getId())
                .dateIssued(p.getDateIssued())
                .status(p.getStatus().name())
                .personId(p.getPerson().getId())
                .pharmacistId(p.getPharmacist().getId())
                .notes(p.getNotes())
                .items(p.getItems().stream()
                        .map(item -> PrescriptionItemDto.builder()
                                .id(item.getId())
                                .medicineId(item.getMedicine().getId())
                                .quantity(item.getQuantity())
                                .dosage(item.getDosage())
                                .frequency(item.getFrequency())
                                .duration(item.getDuration())
                                .instructions(item.getInstructions())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}