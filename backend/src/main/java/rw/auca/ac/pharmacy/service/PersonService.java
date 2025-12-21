package rw.auca.ac.pharmacy.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.PersonDto;
import rw.auca.ac.pharmacy.dto.PersonRequest;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.dto.location.LocationPathDto;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PersonService {
    private static final String PERSON_NOT_FOUND_MSG = "Person not found with ID: ";
    private static final String ANONYMOUS_EMAIL = "anonymous@pharmacy.local";
    private static final String ANONYMOUS_NAME = "Walk-in Customer";

    private final PersonRepository repository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;

    public @NonNull Page<PersonDto> findAll(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    private static class PersonSpecifications {
        public static Specification<Person> hasRole(Role role) {
            return (root, query, cb) -> cb.equal(root.get("role"), role);
        }

        public static Specification<Person> nameContains(String term) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + term.toLowerCase() + "%");
        }

        public static Specification<Person> emailContains(String term) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + term.toLowerCase() + "%");
        }

        public static Specification<Person> phoneContains(String term) {
            return (root, query, cb) -> cb.like(cb.lower(root.get("phone")), "%" + term.toLowerCase() + "%");
        }

        public static Specification<Person> locationNameContains(String term) {
            return (root, query, cb) -> {
                String like = "%" + term.toLowerCase() + "%";
                Join<Person, Location> loc = root.join("location", JoinType.LEFT);
                // Search location name and parent hierarchy (district, sector, cell, village)
                Predicate currentLoc = cb.like(cb.lower(loc.get("name")), like);
                // Check all parent levels (up to 4 levels:
                // village->cell->sector->district->province)
                Predicate parent1 = cb.like(cb.lower(loc.get("parent").get("name")), like);
                Predicate parent2 = cb.like(cb.lower(loc.get("parent").get("parent").get("name")), like);
                Predicate parent3 = cb.like(cb.lower(loc.get("parent").get("parent").get("parent").get("name")), like);
                Predicate parent4 = cb
                        .like(cb.lower(loc.get("parent").get("parent").get("parent").get("parent").get("name")), like);
                return cb.or(currentLoc, parent1, parent2, parent3, parent4);
            };
        }

        public static Specification<Person> matchesQuery(String term) {
            return (root, query, cb) -> {
                String like = "%" + term.toLowerCase() + "%";
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("name")), like));
                predicates.add(cb.like(cb.lower(root.get("email")), like));
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), like));
                predicates.add(cb.like(cb.lower(root.get("role").as(String.class)), like));
                Join<Person, Location> location = root.join("location", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(cb.coalesce(location.get("name"), "")), like));
                try {
                    Long id = Long.parseLong(term);
                    predicates.add(cb.equal(root.get("id"), id));
                    predicates.add(cb.equal(location.get("id"), id));
                } catch (NumberFormatException ex) {
                    // ignore non-numeric
                }
                return cb.or(predicates.toArray(new Predicate[0]));
            };
        }
    }

    public @NonNull Page<PersonDto> findAll(@NonNull Pageable pageable, String id, String name, String email, String phone,
            Role role, String location, String q) {
        String nameTerm = (name != null && !name.isBlank()) ? name.trim() : null;
        String emailTerm = (email != null && !email.isBlank()) ? email.trim() : null;
        String phoneTerm = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        String locationTerm = (location != null && !location.isBlank()) ? location.trim() : null;
        String qTerm = (q != null && !q.isBlank()) ? q.trim() : null;

        if ((id == null || id.isBlank()) && nameTerm == null && emailTerm == null && phoneTerm == null && role == null
                && locationTerm == null && qTerm == null) {
            return repository.findAll(pageable).map(this::toDto);
        }

        Specification<Person> spec = Specification.allOf();
        if (id != null && !id.isBlank()) {
            String idTerm = id.trim();
            spec = spec.and((root, query, cb) -> cb.like(cb.toString(root.get("id")), idTerm + "%"));
        }
        if (qTerm != null) {
            spec = spec.and(PersonSpecifications.matchesQuery(qTerm));
        }
        if (role != null) {
            spec = spec.and(PersonSpecifications.hasRole(role));
        }
        if (nameTerm != null) {
            spec = spec.and(PersonSpecifications.nameContains(nameTerm));
        }
        if (emailTerm != null) {
            spec = spec.and(PersonSpecifications.emailContains(emailTerm));
        }
        if (phoneTerm != null) {
            spec = spec.and(PersonSpecifications.phoneContains(phoneTerm));
        }
        if (locationTerm != null) {
            spec = spec.and(PersonSpecifications.locationNameContains(locationTerm));
        }
        return repository.findAll(spec, pageable).map(this::toDto);
    }

    public Person getOrCreateAnonymousPatient() {
        Person existing = repository.findByEmail(ANONYMOUS_EMAIL);
        if (existing != null) {
            return existing;
        }
        Location location = locationRepository.findAll(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No locations available for anonymous patient"));
        Person created = Objects.requireNonNull(Person.builder()
                .name(ANONYMOUS_NAME)
                .email(ANONYMOUS_EMAIL)
                .phone(null)
                .role(Role.PATIENT)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .location(location)
                .build());
        return repository.save(created);
    }

    public PersonDto create(PersonRequest request) {
        validateRequest(request, true);
        Person person = Objects.requireNonNull(Person.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .location(Location.builder().id(request.getLocationId()).build())
                .build());
        Person saved = repository.save(person);
        log.info("Created new person: {}", saved.getName());
        return toDto(saved);
    }

    public PersonDto get(@NonNull Long id) {
        Person person = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + id)));
        assertSelfOrAdmin(person.getId());
        return toDto(person);
    }

    public PersonDto getSelf() {
        Long me = currentUserPersonId();
        if (me == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return get(me);
    }

    public @NonNull Page<PersonDto> getAll(@NonNull Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public PersonDto updateByAdmin(@NonNull Long id, PersonRequest request) {
        validateRequest(request, false);
        Person person = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + id)));
        assertSelfOrAdmin(person.getId());
        person.setName(request.getName().trim());
        person.setEmail(request.getEmail().toLowerCase().trim());
        person.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        person.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            person.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        person.setLocation(Location.builder().id(request.getLocationId()).build());
        Person updated = repository.save(person);
        log.info("Updated person ID {}: {}", id, updated.getName());
        return toDto(updated);
    }

    public PersonDto updateSelf(PersonRequest request) {
        Long currentId = currentUserPersonId();
        if (currentId == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        validateRequest(request, false); // password is not required for update
        Person person = Objects.requireNonNull(repository.findById(currentId)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + currentId)));

        person.setName(request.getName().trim());
        person.setEmail(request.getEmail().toLowerCase().trim());
        person.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        // Note: person.setRole is NOT called.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            person.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        person.setLocation(Location.builder().id(request.getLocationId()).build());

        Person updated = repository.save(person);
        log.info("Updated self for person ID {}: {}", currentId, updated.getName());
        return toDto(updated);
    }

    public void delete(@NonNull Long id) {
        Person person = Objects.requireNonNull(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + id)));
        assertAdmin();
        person.setDeletedAt(LocalDateTime.now());
        repository.save(person);
        log.warn("Soft-deleted person ID {}", id);
    }

    public @NonNull Page<PersonDto> getByProvinceName(String name, @NonNull Pageable pageable) {
        return repository.findByProvinceName(name.trim(), pageable).map(this::toDto);
    }

    public @NonNull Page<PersonDto> getByProvinceCode(String code, @NonNull Pageable pageable) {
        return repository.findByProvinceCode(code.trim(), pageable).map(this::toDto);
    }

    public String getPersonProvince(@NonNull Long personId) {
        Person p = Objects.requireNonNull(repository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + personId)));

        return Optional.ofNullable(p.getLocation())
                .map(loc -> {
                    LocationPathDto path = buildLocationPath(loc);
                    return path.getProvinceName() != null ? path.getProvinceName() : "Unknown province";
                })
                .orElse("Unknown province");
    }

    public LocationPathDto getPersonLocationPath(@NonNull Long personId) {
        Person p = Objects.requireNonNull(repository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException(PERSON_NOT_FOUND_MSG + personId)));
        assertSelfOrAdmin(p.getId());
        return Optional.ofNullable(p.getLocation())
                .map(this::buildLocationPath)
                .orElse(LocationPathDto.builder().build());
    }

    private PersonDto toDto(Person person) {
        Long locationId = Optional.ofNullable(person.getLocation())
                .map(Location::getId)
                .orElse(null);

        return PersonDto.builder()
                .id(person.getId())
                .name(person.getName())
                .email(person.getEmail())
                .phone(person.getPhone())
                .role(person.getRole())
                .locationId(locationId)
                .build();
    }

    private void validateRequest(PersonRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Person request cannot be null");
        }
        if (creating && (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getLocationId() == null) {
            throw new IllegalArgumentException("Location ID is required");
        }
        if (creating && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private LocationPathDto buildLocationPath(Location location) {
        Location current = location;
        LocationPathDto.LocationPathDtoBuilder builder = LocationPathDto.builder();

        while (current != null) {
            LocationType type = current.getType();
            if (type != null) {
                switch (type) {
                    case PROVINCE -> {
                        if (builder.build().getProvinceId() == null) {
                            builder
                                    .provinceId(current.getId())
                                    .provinceCode(current.getCode())
                                    .provinceName(current.getName());
                        }
                    }
                    case DISTRICT -> {
                        if (builder.build().getDistrictId() == null) {
                            builder
                                    .districtId(current.getId())
                                    .districtCode(current.getCode())
                                    .districtName(current.getName());
                        }
                    }
                    case SECTOR -> {
                        if (builder.build().getSectorId() == null) {
                            builder
                                    .sectorId(current.getId())
                                    .sectorCode(current.getCode())
                                    .sectorName(current.getName());
                        }
                    }
                    case CELL -> {
                        if (builder.build().getCellId() == null) {
                            builder
                                    .cellId(current.getId())
                                    .cellCode(current.getCode())
                                    .cellName(current.getName());
                        }
                    }
                    case VILLAGE -> {
                        if (builder.build().getVillageId() == null) {
                            builder
                                    .villageId(current.getId())
                                    .villageCode(current.getCode())
                                    .villageName(current.getName());
                        }
                    }
                    default -> {
                        // no-op
                    }
                }
            }
            current = current.getParent();
        }

        return builder.build();
    }

    private void assertSelfOrAdmin(Long targetPersonId) {
        if (isAdmin()) {
            return; // Admins can do anything
        }
        Long currentId = currentUserPersonId();
        if (currentId == null || !currentId.equals(targetPersonId)) {
            // If you are not an admin, you can only edit yourself.
            throw new AccessDeniedException("Not allowed to access this resource");
        }
    }

    private void assertAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin only");
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return false;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private Long currentUserPersonId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null)
            return null;
        Person p = repository.findByEmail(auth.getName());
        return p != null ? p.getId() : null;
    }
}
