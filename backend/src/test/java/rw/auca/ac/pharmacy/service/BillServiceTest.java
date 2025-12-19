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
        Location province = locationRepository.save(
                Location.builder()
                        .code("TP01")
                        .name("Test Province")
                        .type(LocationType.PROVINCE)
                        .build()
        );

        Person patient = personRepository.save(
                Person.builder()
                        .name("Alice Doe")
                        .email("alice@example.com")
                        .role(Role.PATIENT)
                        .password("pw")
                        .location(province)
                        .build()
        );

        Person pharmacist = personRepository.save(
                Person.builder()
                        .name("Pharm One")
                        .email("pharm@example.com")
                        .role(Role.PHARMACIST)
                        .password("pw")
                        .location(province)
                        .build()
        );

        Prescription rx = prescriptionRepository.save(
                Prescription.builder()
                        .dateIssued(LocalDateTime.now())
                        .status(PrescriptionStatus.PENDING)
                        .person(patient)
                        .pharmacist(pharmacist)
                        .build()
        );

        Bill bill = billRepository.save(
                Bill.builder()
                        .prescription(rx)
                        .status(BillStatus.PENDING)
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentRef("ABC123")
                        .subtotal(BigDecimal.TEN)
                        .discountAmount(BigDecimal.ZERO)
                        .taxAmount(BigDecimal.ZERO)
                        .currency("RWF")
                        .paymentDate(LocalDateTime.now())
                        .build()
        );

        Page<BillDto> result = billService.getAll(PageRequest.of(0, 10), null, null, null, null, "alice");

        assertEquals(1, result.getTotalElements());
        assertEquals(bill.getId(), result.getContent().get(0).getId());
    }
}
