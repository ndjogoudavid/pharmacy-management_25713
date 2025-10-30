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
import rw.auca.ac.pharmacy.model.location.*;
import rw.auca.ac.pharmacy.repository.location.*;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSeeder {

    private final ProvinceRepository provinceRepo;
    private final DistrictRepository districtRepo;
    private final SectorRepository sectorRepo;
    private final CellRepository cellRepo;
    private final VillageRepository villageRepo;

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @PostConstruct
    @Transactional
    public void seed() {
        try {
            if (provinceRepo.count() > 0) {
                log.info("🌍 Rwanda locations already exist. Skipping seeding.");
                return;
            }

            log.info("🌍 Loading Rwanda location data...");
            InputStream stream = new ClassPathResource("data/location/locations.json").getInputStream();

            // Explicit TypeReference – no diamond
            List<LocationRecord> records = mapper.readValue(
                    stream, new TypeReference<List<LocationRecord>>() {}
            );
            if (records == null || records.isEmpty()) {
                log.warn("⚠️ No location data found in JSON file.");
                return;
            }

            log.info("📦 Found {} location records. Seeding now...", records.size());

            Map<String, Province> provinceMap = new HashMap<>();
            Map<String, District> districtMap = new HashMap<>();
            Map<String, Sector> sectorMap = new HashMap<>();
            Map<String, Cell> cellMap = new HashMap<>();
            List<Village> villages = new ArrayList<>();

            for (LocationRecord r : records) {
                // Coerce ALL codes to String – fixed: Explicit s() on every code
                String provinceCode = s(r.provinceCode());
                String districtCode = s(r.districtCode());
                String sectorCode = s(r.sectorCode());
                String cellCode = s(r.cellCode());
                String villageCode = s(r.villageCode());

                // Province
                Province province = provinceMap.get(provinceCode);
                if (province == null) {
                    province = Province.builder()
                            .code(provinceCode)
                            .name(r.provinceName())
                            .build();
                    provinceMap.put(provinceCode, province);
                }

                // District
                District district = districtMap.get(districtCode);
                if (district == null) {
                    district = District.builder()
                            .code(districtCode)
                            .name(r.districtName())
                            .province(province)
                            .build();
                    districtMap.put(districtCode, district);
                }

                // Sector
                Sector sector = sectorMap.get(sectorCode);
                if (sector == null) {
                    sector = Sector.builder()
                            .code(sectorCode)
                            .name(r.sectorName())
                            .district(district)
                            .build();
                    sectorMap.put(sectorCode, sector);
                }

                // Cell
                Cell cell = cellMap.get(cellCode);
                if (cell == null) {
                    cell = Cell.builder()
                            .code(cellCode)
                            .name(r.cellName())
                            .sector(sector)
                            .build();
                    cellMap.put(cellCode, cell);
                }

                // Village
                villages.add(Village.builder()
                        .code(villageCode)
                        .name(r.villageName())
                        .cell(cell)
                        .build());
            }

            // Persist hierarchy
            provinceRepo.saveAll(provinceMap.values());
            districtRepo.saveAll(districtMap.values());
            sectorRepo.saveAll(sectorMap.values());
            cellRepo.saveAll(cellMap.values());
            villageRepo.saveAll(villages);

            log.info("✅ Rwanda location data seeded: {} provinces, {} districts, {} sectors, {} cells, {} villages.",
                    provinceMap.size(), districtMap.size(), sectorMap.size(), cellMap.size(), villages.size());

        } catch (Exception e) {
            log.error("❌ Failed to seed Rwanda locations", e);
        }
    }

    // JSON record: Codes as Object (handles num/str), names as String
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

    // Coerce Object to clean String (handles numbers, ".0" suffixes)
    private static String s(Object o) {
        if (o == null) return null;
        String v = String.valueOf(o).trim();
        if (v.endsWith(".0")) v = v.substring(0, v.length() - 2);
        return v; 
    }
}