package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rw.auca.ac.pharmacy.dto.dashboard.DashboardSummaryDto;
import rw.auca.ac.pharmacy.dto.dashboard.RecentBillDto;
import rw.auca.ac.pharmacy.dto.dashboard.RecentPrescriptionDto;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

        private final PersonRepository personRepository;
        private final MedicineRepository medicineRepository;
        private final PrescriptionRepository prescriptionRepository;
        private final BillRepository billRepository;

    public DashboardSummaryDto summary() {
                if (isAdmin()) {
                        return buildStaffSummary();
                }
                if (isPharmacist()) {
                        return buildPharmacistSummary();
                }
                if (isPatient()) {
                        return buildPatientSummary();
                }
                return emptySummary();
    }

        private DashboardSummaryDto buildStaffSummary() {
                long patients = safe("count patients", () -> personRepository.countByRole(Role.PATIENT), 0L);
                long pharmacists = safe("count pharmacists", () -> personRepository.countByRole(Role.PHARMACIST), 0L);
                long medicines = safe("count medicines", medicineRepository::count, 0L);
                long lowStock = safe("count low stock (own threshold)", medicineRepository::countLowStockByOwnThreshold,
                                0L);
                if (lowStock == 0) {
                        lowStock = safe("count low stock (fallback threshold)",
                                        () -> medicineRepository.countByQuantityLessThanEqual(10), 0L);
                }
                long prescriptions = safe("count prescriptions", prescriptionRepository::count, 0L);
                long filled = safe("count filled prescriptions",
                                () -> prescriptionRepository.countByStatus(PrescriptionStatus.FILLED), 0L);
                long pending = safe("count pending prescriptions",
                                () -> prescriptionRepository.countByStatus(PrescriptionStatus.PENDING), 0L);
                long bills = safe("count bills", billRepository::count, 0L);
                long paidBills = safe("count paid bills", () -> billRepository.countByStatus(BillStatus.PAID), 0L);
                BigDecimal revenue = safe("sum paid revenue", () -> billRepository.sumNetByStatus(BillStatus.PAID),
                                BigDecimal.ZERO);

                List<RecentBillDto> recentBills = safe("recent bills", () -> billRepository
                                .findAllByOrderByPaymentDateDesc(PageRequest.of(0, 5))
                                .map(b -> RecentBillDto.builder()
                                                .id(b.getId())
                                                .prescriptionId(
                                                                b.getPrescription() != null
                                                                                ? b.getPrescription().getId()
                                                                                : null)
                                                .netTotal(b.getNetTotal())
                                                .paymentDate(b.getPaymentDate())
                                                .status(b.getStatus() != null ? b.getStatus().name() : null)
                                                .build())
                                .getContent(), Collections.emptyList());

                List<RecentPrescriptionDto> recentPrescriptions = safe("recent prescriptions",
                                () -> prescriptionRepository
                                                .findAllByOrderByDateIssuedDesc(PageRequest.of(0, 5))
                                                .map(p -> RecentPrescriptionDto.builder()
                                                                .id(p.getId())
                                                                .patientName(
                                                                                p.getPerson() != null ? p.getPerson()
                                                                                                .getName() : "Unknown")
                                                                .walkInName(p.getWalkInName())
                                                                .walkInPhone(p.getWalkInPhone())
                                                                .status(
                                                                                p.getStatus() != null
                                                                                                ? p.getStatus().name()
                                                                                                : PrescriptionStatus.PENDING
                                                                                                                .name())
                                                                .dateIssued(p.getDateIssued())
                                                                .build())
                                                .getContent(),
                                Collections.emptyList());

                return DashboardSummaryDto.builder()
                                .totalPatients(patients)
                                .totalPharmacists(pharmacists)
                                .totalMedicines(medicines)
                                .lowStockMedicines(lowStock)
                                .totalPrescriptions(prescriptions)
                                .filledPrescriptions(filled)
                                .pendingPrescriptions(pending)
                                .totalBills(bills)
                                .paidBills(paidBills)
                                .totalRevenue(revenue)
                                .recentBills(recentBills)
                                .recentPrescriptions(recentPrescriptions)
                                .build();
        }

        private DashboardSummaryDto buildPatientSummary() {
                Long personId = currentUserPersonId();
                if (personId == null) {
                        return emptySummary();
                }

                long prescriptions = safe("count my prescriptions",
                                () -> prescriptionRepository.countByPersonId(personId), 0L);
                long filled = safe("count my filled prescriptions", () -> prescriptionRepository
                                .countByPersonIdAndStatus(personId, PrescriptionStatus.FILLED), 0L);
                long pending = safe("count my pending prescriptions", () -> prescriptionRepository
                                .countByPersonIdAndStatus(personId, PrescriptionStatus.PENDING), 0L);
                long bills = safe("count my bills", () -> billRepository.countByPrescriptionPersonId(personId), 0L);
                long paidBills = safe("count my paid bills",
                                () -> billRepository.countByPrescriptionPersonIdAndStatus(personId, BillStatus.PAID),
                                0L);
                BigDecimal paidTotal = safe("sum my paid revenue",
                                () -> billRepository.sumNetByStatusForPerson(BillStatus.PAID, personId),
                                BigDecimal.ZERO);

                List<RecentBillDto> recentBills = safe("my recent bills", () -> billRepository
                                .findByPrescriptionPersonIdOrderByPaymentDateDesc(personId, PageRequest.of(0, 5))
                                .map(b -> RecentBillDto.builder()
                                                .id(b.getId())
                                                .prescriptionId(
                                                                b.getPrescription() != null
                                                                                ? b.getPrescription().getId()
                                                                                : null)
                                                .netTotal(b.getNetTotal())
                                                .paymentDate(b.getPaymentDate())
                                                .status(b.getStatus() != null ? b.getStatus().name() : null)
                                                .build())
                                .getContent(), Collections.emptyList());

                List<RecentPrescriptionDto> recentPrescriptions = safe("my recent prescriptions",
                                () -> prescriptionRepository
                                                .findByPersonIdOrderByDateIssuedDesc(personId, PageRequest.of(0, 5))
                                                .map(p -> RecentPrescriptionDto.builder()
                                                                .id(p.getId())
                                                                .patientName(
                                                                                p.getPerson() != null ? p.getPerson()
                                                                                                .getName() : "Unknown")
                                                                .walkInName(p.getWalkInName())
                                                                .walkInPhone(p.getWalkInPhone())
                                                                .status(
                                                                                p.getStatus() != null
                                                                                                ? p.getStatus().name()
                                                                                                : PrescriptionStatus.PENDING
                                                                                                                .name())
                                                                .dateIssued(p.getDateIssued())
                                                                .build())
                                                .getContent(),
                                Collections.emptyList());

                return DashboardSummaryDto.builder()
                                .totalPrescriptions(prescriptions)
                                .filledPrescriptions(filled)
                                .pendingPrescriptions(pending)
                                .totalBills(bills)
                                .paidBills(paidBills)
                                .totalRevenue(paidTotal)
                                .recentBills(recentBills)
                                .recentPrescriptions(recentPrescriptions)
                                .build();
        }

        private DashboardSummaryDto emptySummary() {
                return DashboardSummaryDto.builder()
                                .totalPatients(0)
                                .totalPharmacists(0)
                                .totalMedicines(0)
                                .lowStockMedicines(0)
                                .totalPrescriptions(0)
                                .filledPrescriptions(0)
                                .pendingPrescriptions(0)
                                .totalBills(0)
                                .paidBills(0)
                                .totalRevenue(BigDecimal.ZERO)
                                .recentBills(Collections.emptyList())
                                .recentPrescriptions(Collections.emptyList())
                                .build();
        }

        private <T> T safe(String label, Supplier<T> supplier, T fallback) {
                try {
                        return supplier.get();
                } catch (Exception ex) {
                        log.error("Dashboard query failed: {}", label, ex);
                        return fallback;
                }
        }

        private DashboardSummaryDto buildPharmacistSummary() {
                Long pharmacistId = currentUserPersonId();
                if (pharmacistId == null) {
                        return emptySummary();
                }

                long prescriptions = safe("count my prescriptions",
                                () -> prescriptionRepository.countByPharmacistId(pharmacistId), 0L);
                long filled = safe("count my filled prescriptions",
                                () -> prescriptionRepository.countByPharmacistIdAndStatus(
                                                pharmacistId, PrescriptionStatus.FILLED),
                                0L);
                long pending = safe("count my pending prescriptions",
                                () -> prescriptionRepository.countByPharmacistIdAndStatus(
                                                pharmacistId, PrescriptionStatus.PENDING),
                                0L);
                long bills = safe("count my bills",
                                () -> billRepository.countByPrescriptionPharmacistId(pharmacistId), 0L);
                long paidBills = safe("count my paid bills",
                                () -> billRepository.countByPrescriptionPharmacistIdAndStatus(
                                                pharmacistId, BillStatus.PAID),
                                0L);
                BigDecimal revenue = safe("sum my paid revenue",
                                () -> billRepository.sumNetByStatusForPharmacist(BillStatus.PAID, pharmacistId),
                                BigDecimal.ZERO);

                List<RecentBillDto> recentBills = safe("my recent bills", () -> billRepository
                                .findByPrescriptionPharmacistIdOrderByPaymentDateDesc(pharmacistId,
                                                PageRequest.of(0, 5))
                                .map(b -> RecentBillDto.builder()
                                                .id(b.getId())
                                                .prescriptionId(
                                                                b.getPrescription() != null
                                                                                ? b.getPrescription().getId()
                                                                                : null)
                                                .netTotal(b.getNetTotal())
                                                .paymentDate(b.getPaymentDate())
                                                .status(b.getStatus() != null ? b.getStatus().name() : null)
                                                .build())
                                .getContent(), Collections.emptyList());

                List<RecentPrescriptionDto> recentPrescriptions = safe("my recent prescriptions",
                                () -> prescriptionRepository
                                                .findByPharmacistIdOrderByDateIssuedDesc(pharmacistId,
                                                                PageRequest.of(0, 5))
                                                .map(p -> RecentPrescriptionDto.builder()
                                                                .id(p.getId())
                                                                .patientName(
                                                                                p.getPerson() != null ? p.getPerson()
                                                                                                .getName() : "Unknown")
                                                                .walkInName(p.getWalkInName())
                                                                .walkInPhone(p.getWalkInPhone())
                                                                .status(
                                                                                p.getStatus() != null
                                                                                                ? p.getStatus().name()
                                                                                                : PrescriptionStatus.PENDING
                                                                                                                .name())
                                                                .dateIssued(p.getDateIssued())
                                                                .build())
                                                .getContent(),
                                Collections.emptyList());

                return DashboardSummaryDto.builder()
                                .totalPatients(0)
                                .totalPharmacists(0)
                                .totalMedicines(0)
                                .lowStockMedicines(0)
                                .totalPrescriptions(prescriptions)
                                .filledPrescriptions(filled)
                                .pendingPrescriptions(pending)
                                .totalBills(bills)
                                .paidBills(paidBills)
                                .totalRevenue(revenue)
                                .recentBills(recentBills)
                                .recentPrescriptions(recentPrescriptions)
                                .build();
        }

        private boolean isPatient() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null)
                        return false;
                return auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(a -> a.equals("ROLE_PATIENT"));
        }

        private boolean isAdmin() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null)
                        return false;
                return auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        }

        private boolean isPharmacist() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null)
                        return false;
                return auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(a -> a.equals("ROLE_PHARMACIST"));
        }

        private Long currentUserPersonId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getName() == null)
                        return null;
                var p = personRepository.findByEmail(auth.getName());
                return p != null ? p.getId() : null;
        }
}
