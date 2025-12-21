package rw.auca.ac.pharmacy.config;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rw.auca.ac.pharmacy.model.core.Category;
import rw.auca.ac.pharmacy.model.core.Medicine;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.core.PrescriptionItem;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.CategoryRepository;
import rw.auca.ac.pharmacy.repository.MedicineRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Seeds demo data when APP_SEED_DEMO=true. Safe to run on an empty database for presentation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // run after LocationSeeder
public class DemoDataSeeder implements CommandLineRunner {

    @Value("${app.seed.demo:false}")
    private boolean seedDemo;

    @Value("${app.seed.demo.password:Demo123!}")
    private String demoPassword;

    private final PersonRepository personRepository;
    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemo) {
            return;
        }
        log.info("APP_SEED_DEMO=true: seeding demo data...");

        Location anyLocation = resolveLocation();
        if (anyLocation == null) {
            log.warn("APP_SEED_DEMO enabled but no locations are present; skipping demo seed. Ensure LocationSeeder ran.");
            return;
        }

        ensureUser("admin@demo.com", "Admin User", Role.ADMIN, anyLocation);
        Person pharmacist = ensureUser("pharma@demo.com", "Pharma Demo", Role.PHARMACIST, anyLocation);
        Person patient = ensureUser("patient@demo.com", "Patient Demo", Role.PATIENT, anyLocation);

        Category cat1 = categoryRepository.findByName("Antibiotics")
                .orElseGet(() -> {
                    Category category = Objects.requireNonNull(Category.builder()
                            .name("Antibiotics")
                            .description("Demo antibiotics")
                            .build());
                    return categoryRepository.save(category);
                });
        Category cat2 = categoryRepository.findByName("Pain Relief")
                .orElseGet(() -> {
                    Category category = Objects.requireNonNull(Category.builder()
                            .name("Pain Relief")
                            .description("Demo analgesics")
                            .build());
                    return categoryRepository.save(category);
                });

        Medicine med1 = ensureMedicine("AMOX-001", "Amoxicillin", 100, BigDecimal.valueOf(12_00), List.of(cat1));
        Medicine med2 = ensureMedicine("IBU-200", "Ibuprofen 200mg", 200, BigDecimal.valueOf(5_00), List.of(cat2));

        // Prescriptions
        if (prescriptionRepository.count() == 0) {
            Prescription pending = Objects.requireNonNull(Prescription.builder()
                    .dateIssued(LocalDateTime.now().minusDays(1))
                    .status(PrescriptionStatus.PENDING)
                    .person(patient)
                    .pharmacist(pharmacist)
                    .notes("Demo pending prescription")
                    .build());
            Prescription savedPending = prescriptionRepository.save(pending);
            prescriptionRepository.save(addItem(savedPending, med1, 2, "500mg", "TID", "5 days", "After meals"));

            Prescription filled = Objects.requireNonNull(Prescription.builder()
                    .dateIssued(LocalDateTime.now().minusDays(2))
                    .status(PrescriptionStatus.FILLED)
                    .person(patient)
                    .pharmacist(pharmacist)
                    .notes("Demo filled prescription")
                    .build());
            Prescription savedFilled = prescriptionRepository.save(filled);
            Prescription filledWithItems = addItem(savedFilled, med2, 1, "200mg", "BID", "3 days", "With water");
            filledWithItems.getItems().forEach(item -> {
                item.setDispensed(true);
                item.setDispensedQuantity(item.getQuantity());
            });
            prescriptionRepository.save(filledWithItems);
        }
    }

    private Person ensureUser(String email, String name, Role role, Location loc) {
        return Optional.ofNullable(personRepository.findByEmail(email))
                .orElseGet(() -> {
                    Person person = Objects.requireNonNull(Person.builder()
                            .email(email)
                            .name(name)
                            .role(role)
                            .password(passwordEncoder.encode(demoPassword))
                            .location(loc)
                            .build());
                    return personRepository.save(person);
                });
    }

    private Medicine ensureMedicine(String sku, String name, int quantity, BigDecimal price, List<Category> categories) {
        return medicineRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name).stream().findFirst()
                .orElseGet(() -> {
                    Medicine m = Medicine.builder()
                            .sku(sku)
                            .name(name)
                            .quantity(quantity)
                            .reorderLevel(5)
                            .price(price.movePointLeft(2)) // store as decimal with 2 digits
                            .isControlled(false)
                            .build();
                    m.getCategories().addAll(categories);
                    return medicineRepository.save(m);
                });
    }

    private Prescription addItem(Prescription rx, Medicine med, int qty, String dosage, String frequency, String duration, String instructions) {
        PrescriptionItem item = PrescriptionItem.builder()
                .prescription(rx)
                .medicine(med)
                .quantity(qty)
                .dosage(dosage)
                .frequency(frequency)
                .duration(duration)
                .instructions(instructions)
                .build();
        rx.getItems().add(item);
        return rx;
    }

    private Location resolveLocation() {
        return locationRepository.findByType(LocationType.VILLAGE).stream().findFirst()
                .or(() -> locationRepository.findByType(LocationType.CELL).stream().findFirst())
                .or(() -> locationRepository.findByType(LocationType.SECTOR).stream().findFirst())
                .or(() -> locationRepository.findByType(LocationType.DISTRICT).stream().findFirst())
                .or(() -> locationRepository.findByType(LocationType.PROVINCE).stream().findFirst())
                .orElse(null);
    }
}
