package rw.auca.ac.pharmacy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.model.enums.LocationType;
import rw.auca.ac.pharmacy.model.location.Location;
import rw.auca.ac.pharmacy.repository.location.LocationRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds Rwanda administrative hierarchy into a single self-referencing Location table.
 * It reads src/main/resources/data/location/locations.json and builds
 * PROVINCE -> DISTRICT -> SECTOR -> CELL -> VILLAGE records with parent links.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSeeder {

    private final LocationRepository locationRepo;

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @PostConstruct
    @Transactional
    public void seed() {
        try {
            if (locationRepo.count() > 0) {
                log.info("Locations already exist. Skipping self-referencing seeding.");
                return;
            }

            log.info("Loading Rwanda location data (self-referencing Location)...");
            InputStream stream = new ClassPathResource("data/location/locations.json").getInputStream();

            List<LocationRecord> records = mapper.readValue(stream, new TypeReference<>() {});

            if (records == null || records.isEmpty()) {
                log.warn("No location data found in JSON file.");
                return;
            }

            Map<String, Location> provinces = new LinkedHashMap<>();
            Map<String, Location> districts = new LinkedHashMap<>();
            Map<String, Location> sectors = new LinkedHashMap<>();
            Map<String, Location> cells = new LinkedHashMap<>();
            List<Location> villages = new ArrayList<>();

            int total = records.size();
            int skipped = 0;

            for (LocationRecord r : records) {
                String pCode = padLeft(normalizeDigits(r.provinceCode()), 2);
                String dCode = padLeft(normalizeDigits(r.districtCode()), 3);
                String sCode = padLeft(normalizeDigits(r.sectorCode()), 6);
                String cCode = padLeft(normalizeDigits(r.cellCode()), 8);
                String vCode = padLeft(normalizeDigits(r.villageCode()), 9);

                if (pCode == null || dCode == null || sCode == null || cCode == null || vCode == null) {
                    skipped++;
                    continue;
                }

                Location province = provinces.computeIfAbsent(pCode,
                        k -> Location.builder()
                                .code(pCode)
                                .name(safe(r.provinceName()))
                                .type(LocationType.PROVINCE)
                                .build());

                Location district = districts.computeIfAbsent(dCode,
                        k -> Location.builder()
                                .code(dCode)
                                .name(safe(r.districtName()))
                                .type(LocationType.DISTRICT)
                                .parent(province)
                                .build());

                Location sector = sectors.computeIfAbsent(sCode,
                        k -> Location.builder()
                                .code(sCode)
                                .name(safe(r.sectorName()))
                                .type(LocationType.SECTOR)
                                .parent(district)
                                .build());

                Location cell = cells.computeIfAbsent(cCode,
                        k -> Location.builder()
                                .code(cCode)
                                .name(safe(r.cellName()))
                                .type(LocationType.CELL)
                                .parent(sector)
                                .build());

                villages.add(Location.builder()
                        .code(vCode)
                        .name(safe(r.villageName()))
                        .type(LocationType.VILLAGE)
                        .parent(cell)
                        .build());
            }

            locationRepo.saveAll(new ArrayList<>(provinces.values()));
            locationRepo.saveAll(new ArrayList<>(districts.values()));
            locationRepo.saveAll(new ArrayList<>(sectors.values()));
            locationRepo.saveAll(new ArrayList<>(cells.values()));
            locationRepo.saveAll(new ArrayList<>(villages));

            log.info("Seeded locations (self-referencing). Raw rows: {} | Skipped: {} | Provinces: {} | Districts: {} | Sectors: {} | Cells: {} | Villages: {}",
                    total, skipped, provinces.size(), districts.size(), sectors.size(), cells.size(), villages.size());

        } catch (Exception e) {
            log.error("Failed to seed Rwanda locations", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocationRecord(
            Object provinceCode,
            String provinceName,
            Object districtCode,
            String districtName,
            Object sectorCode,
            String sectorName,
            Object cellCode,
            String cellName,
            Object villageCode,
            String villageName
    ) {}

    private static String normalizeDigits(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return Long.toString(n.longValue());
        String s = o.toString().trim();
        if (s.matches("^\\d+\\.0+$")) s = s.substring(0, s.indexOf('.'));
        s = s.replaceAll("\\D", "");
        return s.isEmpty() ? null : s;
    }

    private static String padLeft(String v, int length) {
        if (v == null) return null;
        if (v.length() >= length) return v;
        return "0".repeat(length - v.length()) + v;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
