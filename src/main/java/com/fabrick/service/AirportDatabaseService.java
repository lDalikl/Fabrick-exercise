package com.fabrick.service;

import com.fabrick.entity.AirportCacheEntity;
import com.fabrick.entity.StationCacheEntity;
import com.fabrick.repository.AirportCacheRepository;
import com.fabrick.repository.StationCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirportDatabaseService {

    private final AirportCacheRepository airportCacheRepository;
    private final StationCacheRepository stationCacheRepository;

    /**
     * Load airports database from CSV file on application startup.
     * This method is automatically called after dependency injection is complete.
     */
    @PostConstruct
    public void loadAirportsDatabase() {
        try {
            log.info("Loading airports database from CSV...");

            ClassPathResource resource = new ClassPathResource("data/airports.csv");
            if (!resource.exists()) {
                log.warn("airports.csv not found, skipping database load");
                log.info("Download from: https://davidmegginson.github.io/ourairports-data/airports.csv");
                return;
            }

            List<AirportCacheEntity> airports = new ArrayList<>();
            List<StationCacheEntity> stations = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {

                // Skip header line
                String line = reader.readLine();
                int count = 0;

                // Process each data line
                while ((line = reader.readLine()) != null) {
                    String[] fields = parseCsvLine(line);

                    if (fields.length < 13) continue;

                    // Extract relevant fields from CSV
                    String type = fields[2];        // type (e.g., "large_airport")
                    String name = fields[3];        // name
                    String latitude = fields[4];    // latitude_deg
                    String longitude = fields[5];   // longitude_deg
                    String elevation = fields[6];   // elevation_ft
                    String isoCountry = fields[8];  // iso_country (e.g., "US")
                    String isoRegion = fields[9];   // iso_region (e.g., "US-CO")
                    String gpsCode = fields[12];    // gps_code
                    String icaoCode = fields[1];    // ident (ICAO code)

                    // Filter: only valid ICAO codes (4 chars) and airport/heliport types
                    if (!icaoCode.isEmpty() && icaoCode.length() == 4 &&
                            (type.contains("airport") || type.contains("heliport"))) {

                        try {
                            Double lat = parseDouble(latitude);
                            Double lon = parseDouble(longitude);
                            Double elev = parseDouble(elevation);

                            // Require valid coordinates
                            if (lat != null && lon != null) {
                                // Extract state from iso_region (format: "US-CO" -> "CO")
                                String state = "";
                                if (isoRegion.contains("-")) {
                                    state = isoRegion.split("-")[1];
                                }

                                // Create airport entity
                                AirportCacheEntity airport = AirportCacheEntity.builder()
                                        .airportId(icaoCode)
                                        .name(name)
                                        .state(state)
                                        .country(isoCountry)
                                        .latitude(lat)
                                        .longitude(lon)
                                        .elevation(elev)
                                        .cachedAt(LocalDateTime.now())
                                        .build();

                                airports.add(airport);

                                // Create station entity (same data, different table)
                                StationCacheEntity station = StationCacheEntity.builder()
                                        .stationId(icaoCode)
                                        .site(name)
                                        .state(state)
                                        .country(isoCountry)
                                        .latitude(lat)
                                        .longitude(lon)
                                        .elevation(elev != null ? elev.intValue() : null)
                                        .cachedAt(LocalDateTime.now())
                                        .build();

                                stations.add(station);

                                count++;
                            }
                        } catch (Exception e) {
                            // Silently skip invalid records
                        }
                    }
                }

                // Batch save to database for performance
                log.info("Saving {} airports to database...", airports.size());
                airportCacheRepository.saveAll(airports);

                log.info("Saving {} stations to database...", stations.size());
                stationCacheRepository.saveAll(stations);

                log.info("Successfully loaded {} airports/stations from CSV", count);

            }
        } catch (Exception e) {
            log.error("Error loading airports database", e);
        }
    }

    /**
     * Parse CSV line handling quoted fields correctly.
     * Handles cases like: field1,"field with, comma",field3
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    /**
     * Safely parse string to Double, returning null for invalid values.
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}