package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rw.auca.ac.pharmacy.dto.search.GlobalSearchResultDto;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Category;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.CategoryRepository;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final MedicineRepository medicineRepository;
    private final PersonRepository personRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final BillRepository billRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;

    public List<GlobalSearchResultDto> search(String q, String type, boolean byId) {
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        String query = q.trim();

        String normalizedType = type != null && !type.isBlank()
                ? type.trim().toUpperCase(Locale.ROOT)
                : "ALL";

        boolean admin = isAdmin();
        boolean pharmacist = isPharmacist();
        boolean patient = isPatient();
        Person me = currentUserPerson();
        Long myId = me != null ? me.getId() : null;

        List<GlobalSearchResultDto> results = new ArrayList<>();

        if (byId) {
            // ID-only search: treat q as numeric ID and look up exact matches
            try {
                Long id = Long.parseLong(query);

                if (normalizedType.equals("ALL") || normalizedType.equals("MEDICINE")) {
                    medicineRepository.findById(id).ifPresent(m -> results.add(GlobalSearchResultDto.builder()
                            .type("MEDICINE")
                            .id(m.getId())
                            .label(m.getName())
                            .description(m.getSku())
                            .subtitle(m.getSku())
                            .url("/medicines?focus=" + m.getId())
                            .build()));
                }

                if (normalizedType.equals("ALL") || normalizedType.equals("PERSON")) {
                    if (patient) {
                        if (myId != null && myId.equals(id) && me != null) {
                            results.add(GlobalSearchResultDto.builder()
                                    .type("PERSON")
                                    .id(me.getId())
                                    .label(me.getName())
                                    .description(me.getEmail())
                                    .subtitle(me.getEmail())
                                    .url("/profile")
                                    .build());
                        }
                    } else {
                        personRepository.findById(id).ifPresent(p -> results.add(GlobalSearchResultDto.builder()
                                .type("PERSON")
                                .id(p.getId())
                                .label(p.getName())
                                .description(p.getEmail())
                                .subtitle(p.getEmail())
                                .url("/people?focus=" + p.getId())
                                .build()));
                    }
                }

                if (normalizedType.equals("ALL") || normalizedType.equals("PRESCRIPTION")) {
                    prescriptionRepository.findById(id).ifPresent(p -> {
                        if (patient) {
                            if (myId == null || p.getPerson() == null || !myId.equals(p.getPerson().getId())) {
                                return;
                            }
                        } else if (pharmacist) {
                            if (myId == null || p.getPharmacist() == null
                                    || !myId.equals(p.getPharmacist().getId())) {
                                return;
                            }
                        }
                        results.add(GlobalSearchResultDto.builder()
                                .type("PRESCRIPTION")
                                .id(p.getId())
                                .label("Rx #" + p.getId())
                                .description(p.getPerson() != null ? p.getPerson().getName() : "")
                                .subtitle(p.getPerson() != null ? p.getPerson().getName() : "")
                                .url("/prescriptions?focus=" + p.getId())
                                .build());
                    });
                }

                if (normalizedType.equals("ALL") || normalizedType.equals("BILL")) {
                    billRepository.findById(id).ifPresent(b -> {
                        if (patient) {
                            if (myId == null || b.getPrescription() == null || b.getPrescription().getPerson() == null
                                    || !myId.equals(b.getPrescription().getPerson().getId())) {
                                return;
                            }
                        } else if (pharmacist) {
                            if (myId == null || b.getPrescription() == null
                                    || b.getPrescription().getPharmacist() == null
                                    || !myId.equals(b.getPrescription().getPharmacist().getId())) {
                                return;
                            }
                        }
                        results.add(GlobalSearchResultDto.builder()
                                .type("BILL")
                                .id(b.getId())
                                .label("Bill #" + b.getId())
                                .description(b.getPaymentRef())
                                .subtitle(b.getStatus() != null ? b.getStatus().name() : "")
                                .url("/bills?focus=" + b.getId())
                                .build());
                    });
                }

                if (admin && (normalizedType.equals("ALL") || normalizedType.equals("LOCATION"))) {
                    locationRepository.findById(id).ifPresent(l -> results.add(GlobalSearchResultDto.builder()
                            .type("LOCATION")
                            .id(l.getId())
                            .label(l.getName())
                            .description(l.getCode())
                            .subtitle(l.getType() != null ? l.getType().name() : "")
                            .url("/locations?focus=" + l.getId())
                            .build()));
                }

                if (!patient && (normalizedType.equals("ALL") || normalizedType.equals("CATEGORY"))) {
                    categoryRepository.findById(id).ifPresent(c -> results.add(GlobalSearchResultDto.builder()
                            .type("CATEGORY")
                            .id(c.getId())
                            .label(c.getName())
                            .description(c.getDescription())
                            .subtitle("Category")
                            .url("/categories?focus=" + c.getId())
                            .build()));
                }
            } catch (NumberFormatException ex) {
                // Non-numeric input with byId=true -> no results
                return List.of();
            }

            return results;
        }

        // Text mode search (original behaviour), optionally narrowed by type
        if (normalizedType.equals("ALL") || normalizedType.equals("MEDICINE")) {
            List<Medicine> meds = medicineRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query);
            meds.forEach(m -> results.add(GlobalSearchResultDto.builder()
                    .type("MEDICINE")
                    .id(m.getId())
                    .label(m.getName())
                    .description(m.getSku())
                    .subtitle(m.getSku())
                    .url("/medicines?focus=" + m.getId())
                    .build()));
        }

        if (normalizedType.equals("ALL") || normalizedType.equals("PERSON")) {
            if (patient) {
                if (me != null) {
                    String needle = query.toLowerCase(Locale.ROOT);
                    String name = me.getName() != null ? me.getName().toLowerCase(Locale.ROOT) : "";
                    String email = me.getEmail() != null ? me.getEmail().toLowerCase(Locale.ROOT) : "";
                    if (name.contains(needle) || email.contains(needle)) {
                        results.add(GlobalSearchResultDto.builder()
                                .type("PERSON")
                                .id(me.getId())
                                .label(me.getName())
                                .description(me.getEmail())
                                .subtitle(me.getRole() != null ? me.getRole().name() : me.getEmail())
                                .url("/profile")
                                .build());
                    }
                }
            } else {
                List<Person> people = personRepository
                        .findTop20ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
                                query,
                                query);
                people.forEach(p -> results.add(GlobalSearchResultDto.builder()
                        .type("PERSON")
                        .id(p.getId())
                        .label(p.getName())
                        .description(p.getEmail())
                        .subtitle(p.getRole() != null ? p.getRole().name() : p.getEmail())
                        .url("/people?focus=" + p.getId())
                        .build()));
            }
        }

        if (normalizedType.equals("ALL") || normalizedType.equals("PRESCRIPTION")) {
            if (patient) {
                if (myId != null) {
                    prescriptionRepository.searchForTerm(query, PageRequest.of(0, 10))
                            .stream()
                            .filter(p -> p.getPerson() != null && myId.equals(p.getPerson().getId()))
                            .forEach(p -> results.add(GlobalSearchResultDto.builder()
                                    .type("PRESCRIPTION")
                                    .id(p.getId())
                                    .label("Rx #" + p.getId())
                                    .description(p.getPerson() != null ? p.getPerson().getName() : "")
                                    .subtitle(p.getStatus() != null ? p.getStatus().name() : "")
                                    .url("/prescriptions?focus=" + p.getId())
                                    .build()));
                }
            } else if (pharmacist) {
                if (myId != null) {
                    prescriptionRepository.searchForTerm(query, PageRequest.of(0, 10))
                            .stream()
                            .filter(p -> p.getPharmacist() != null && myId.equals(p.getPharmacist().getId()))
                            .forEach(p -> results.add(GlobalSearchResultDto.builder()
                                    .type("PRESCRIPTION")
                                    .id(p.getId())
                                    .label("Rx #" + p.getId())
                                    .description(p.getPerson() != null ? p.getPerson().getName() : "")
                                    .subtitle(p.getStatus() != null ? p.getStatus().name() : "")
                                    .url("/prescriptions?focus=" + p.getId())
                                    .build()));
                }
            } else {
                prescriptionRepository.searchForTerm(query, PageRequest.of(0, 10))
                        .forEach(p -> results.add(GlobalSearchResultDto.builder()
                                .type("PRESCRIPTION")
                                .id(p.getId())
                                .label("Rx #" + p.getId())
                                .description(p.getPerson() != null ? p.getPerson().getName() : "")
                                .subtitle(p.getStatus() != null ? p.getStatus().name() : "")
                                .url("/prescriptions?focus=" + p.getId())
                                .build()));
            }
        }

        if (normalizedType.equals("ALL") || normalizedType.equals("BILL")) {
            if (patient) {
                if (myId != null) {
                    String needle = query.toLowerCase(Locale.ROOT);
                    List<Bill> bills = billRepository
                            .findByPrescriptionPersonIdOrderByPaymentDateDesc(myId, PageRequest.of(0, 10))
                            .getContent();
                    bills.stream()
                            .filter(b -> {
                                String ref = b.getPaymentRef() != null ? b.getPaymentRef().toLowerCase(Locale.ROOT)
                                        : "";
                                return ref.contains(needle);
                            })
                            .forEach(b -> results.add(GlobalSearchResultDto.builder()
                                    .type("BILL")
                                    .id(b.getId())
                                    .label("Bill #" + b.getId())
                                    .description(b.getPaymentRef())
                                    .subtitle(b.getStatus() != null ? b.getStatus().name() : "")
                                    .url("/bills?focus=" + b.getId())
                                    .build()));
                }
            } else if (pharmacist) {
                if (myId != null) {
                    List<Bill> bills = billRepository
                            .findByPaymentRefContainingIgnoreCaseOrderByPaymentDateDesc(query, PageRequest.of(0, 10))
                            .getContent();
                    bills.stream()
                            .filter(b -> b.getPrescription() != null && b.getPrescription().getPharmacist() != null
                                    && myId.equals(b.getPrescription().getPharmacist().getId()))
                            .forEach(b -> results.add(GlobalSearchResultDto.builder()
                                    .type("BILL")
                                    .id(b.getId())
                                    .label("Bill #" + b.getId())
                                    .description(b.getPaymentRef())
                                    .subtitle(b.getStatus() != null ? b.getStatus().name() : "")
                                    .url("/bills?focus=" + b.getId())
                                    .build()));
                }
            } else {
                List<Bill> bills = billRepository
                        .findByPaymentRefContainingIgnoreCaseOrderByPaymentDateDesc(query, PageRequest.of(0, 10))
                        .getContent();
                bills.forEach(b -> results.add(GlobalSearchResultDto.builder()
                        .type("BILL")
                        .id(b.getId())
                        .label("Bill #" + b.getId())
                        .description(b.getPaymentRef())
                        .subtitle(b.getStatus() != null ? b.getStatus().name() : "")
                        .url("/bills?focus=" + b.getId())
                        .build()));
            }
        }

        if (admin && (normalizedType.equals("ALL") || normalizedType.equals("LOCATION"))) {
            List<Location> locations = locationRepository
                    .findByNameContainingIgnoreCase(query, PageRequest.of(0, 10))
                    .getContent();
            locations.forEach(l -> results.add(GlobalSearchResultDto.builder()
                    .type("LOCATION")
                    .id(l.getId())
                    .label(l.getName())
                    .description(l.getCode())
                    .subtitle(l.getType() != null ? l.getType().name() : "")
                    .url("/locations?focus=" + l.getId())
                    .build()));
        }

        if (!patient && (normalizedType.equals("ALL") || normalizedType.equals("CATEGORY"))) {
            List<Category> categories = categoryRepository
                    .findTop20ByNameContainingIgnoreCaseOrderByNameAsc(query);
            categories.forEach(c -> results.add(GlobalSearchResultDto.builder()
                    .type("CATEGORY")
                    .id(c.getId())
                    .label(c.getName())
                    .description(c.getDescription())
                    .subtitle("Category")
                    .url("/categories?focus=" + c.getId())
                    .build()));
        }

        return results;
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

    private Person currentUserPerson() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null)
            return null;
        return personRepository.findByEmail(auth.getName());
    }
}
