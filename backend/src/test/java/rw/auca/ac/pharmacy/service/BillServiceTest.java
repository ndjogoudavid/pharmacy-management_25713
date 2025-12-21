package rw.auca.ac.pharmacy.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import rw.auca.ac.pharmacy.dto.BillDto;
import rw.auca.ac.pharmacy.model.core.Bill;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.model.core.Prescription;
import rw.auca.ac.pharmacy.model.enums.BillStatus;
import rw.auca.ac.pharmacy.model.enums.PaymentMethod;
import rw.auca.ac.pharmacy.model.enums.PrescriptionStatus;
import rw.auca.ac.pharmacy.model.enums.Role;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.BillRepository;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.repository.PrescriptionRepository;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BillServiceTest {

        @Autowired
        private BillService billService;

        @Autowired
        private BillRepository billRepository;

        @Autowired
        private PersonRepository personRepository;

        @Autowired
        private PrescriptionRepository prescriptionRepository;

        @Autowired
        private LocationRepository locationRepository;

        @Test
        void filtersBillsByPatientNameWithPagination() {
                Location province = Objects.requireNonNull(Location.builder()
                                .code("TP01")
                                .name("Test Province")
                                .type(LocationType.PROVINCE)
                                .build());
                province = locationRepository.save(province);

                Person patient = Objects.requireNonNull(Person.builder()
                                .name("Alice Doe")
                                .email("alice@example.com")
                                .role(Role.PATIENT)
                                .password("pw")
                                .location(province)
                                .build());
                patient = personRepository.save(patient);

                Person pharmacist = Objects.requireNonNull(Person.builder()
                                .name("Pharm One")
                                .email("pharm@example.com")
                                .role(Role.PHARMACIST)
                                .password("pw")
                                .location(province)
                                .build());
                pharmacist = personRepository.save(pharmacist);

                Prescription rx = Objects.requireNonNull(Prescription.builder()
                                .dateIssued(LocalDateTime.now())
                                .status(PrescriptionStatus.PENDING)
                                .person(patient)
                                .pharmacist(pharmacist)
                                .build());
                rx = prescriptionRepository.save(rx);

                Bill bill = Objects.requireNonNull(Bill.builder()
                                .prescription(rx)
                                .status(BillStatus.PENDING)
                                .paymentMethod(PaymentMethod.CASH)
                                .paymentRef("ABC123")
                                .subtotal(BigDecimal.TEN)
                                .discountAmount(BigDecimal.ZERO)
                                .taxAmount(BigDecimal.ZERO)
                                .currency("RWF")
                                .paymentDate(LocalDateTime.now())
                                .build());
                bill = billRepository.save(bill);

                Page<BillDto> result = billService.getAll(PageRequest.of(0, 10), null, null, null, null, null, "alice",
                                null);

                assertEquals(1, result.getTotalElements());
                assertEquals(bill.getId(), result.getContent().get(0).getId());
        }
}
