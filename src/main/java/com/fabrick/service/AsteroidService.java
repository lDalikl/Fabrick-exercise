package com.fabrick.service;

import com.fabrick.model.AsteroidPath;
import com.fabrick.dto.NasaAsteroidResponse;
import com.fabrick.dto.NasaAsteroidResponse.CloseApproachData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsteroidService {

    private final WebClient nasaWebClient;

    @Value("${nasa.api.key:DEMO_KEY}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Retrieve asteroid path data showing transitions between orbiting bodies.
     * The method fetches close approach data from NASA API, filters by date range,
     * and identifies when the asteroid transitions from one planet to another.
     */
    @Cacheable(value = "asteroids", key = "#asteroidId + '-' + #fromDate + '-' + #toDate")
    public Mono<List<AsteroidPath>> getAsteroidPaths(String asteroidId, LocalDate fromDate, LocalDate toDate) {
        log.info("Fetching asteroid paths for ID: {}, from: {}, to: {}", asteroidId, fromDate, toDate);

        return nasaWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/neo/{asteroidId}")
                        .queryParam("api_key", apiKey)
                        .build(asteroidId))
                .retrieve()
                .bodyToMono(NasaAsteroidResponse.class)
                .map(response -> this.processAsteroidData(response, fromDate, toDate))
                .doOnError(error -> log.error("Error fetching asteroid data", error))
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * Process NASA API response to extract planetary transition paths.
     * Logic:
     * 1. Filter approaches within the specified date range
     * 2. Sort approaches chronologically
     * 3. Detect planet changes (e.g., from Jupiter to Earth)
     * 4. Create path objects for each transition with from/to dates
     */
    private List<AsteroidPath> processAsteroidData(NasaAsteroidResponse response,
                                                   LocalDate fromDate,
                                                   LocalDate toDate) {
        List<AsteroidPath> paths = new ArrayList<>();
        List<CloseApproachData> approaches = response.getCloseApproachData();

        if (approaches == null || approaches.isEmpty()) {
            return paths;
        }

        // Filter approaches within date range
        List<CloseApproachData> filteredApproaches = new ArrayList<>();
        for (CloseApproachData approach : approaches) {
            LocalDate approachDate = LocalDate.parse(approach.getCloseApproachDate(), DATE_FORMATTER);
            boolean isAfterOrEqualFromDate = !approachDate.isBefore(fromDate);
            boolean isBeforeOrEqualToDate = !approachDate.isAfter(toDate);

            if (isAfterOrEqualFromDate && isBeforeOrEqualToDate) {
                filteredApproaches.add(approach);
            }
        }

        // Sort approaches chronologically
        this.sortApproachesByDate(filteredApproaches);

        // Build paths based on planetary transitions
        String currentPlanet = null;
        String currentFromDate = null;

        for (CloseApproachData approach : filteredApproaches) {
            String planet = approach.getOrbitingBody();
            String date = approach.getCloseApproachDate();

            if (currentPlanet == null) {
                // First approach - initialize tracking variables
                currentPlanet = planet;
                currentFromDate = date;
            } else if (!planet.equals(currentPlanet)) {
                // Planet change detected - create path object
                AsteroidPath newPath = new AsteroidPath();
                newPath.setFromPlanet(currentPlanet);
                newPath.setToPlanet(planet);
                newPath.setFromDate(currentFromDate);
                newPath.setToDate(date);
                paths.add(newPath);

                // Update tracking for next transition
                currentPlanet = planet;
                currentFromDate = date;
            }
        }

        return paths;
    }

    /**
     * Sort close approach data chronologically using bubble sort.
     * Simple implementation suitable for small datasets (typically < 100 entries per asteroid).
     */
    private void sortApproachesByDate(List<CloseApproachData> approaches) {
        for (int i = 0; i < approaches.size(); i++) {
            for (int j = i + 1; j < approaches.size(); j++) {
                CloseApproachData first = approaches.get(i);
                CloseApproachData second = approaches.get(j);
                String firstDate = first.getCloseApproachDate();
                String secondDate = second.getCloseApproachDate();

                if (secondDate.compareTo(firstDate) < 0) {
                    // Swap elements if second date is earlier
                    approaches.set(i, second);
                    approaches.set(j, first);
                }
            }
        }
    }
}