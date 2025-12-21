package rw.auca.ac.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "rw.auca.ac.pharmacy.model")
@EnableJpaRepositories(basePackages = "rw.auca.ac.pharmacy.repository")
public class PharmacyManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(PharmacyManagementApplication.class, args);
    }
}
