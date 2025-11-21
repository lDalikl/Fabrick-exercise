package com.fabrick.controller;

import com.fabrick.model.Airport;
import com.fabrick.model.AsteroidPath;
import com.fabrick.model.Station;
import com.fabrick.service.AirportStationService;
import com.fabrick.service.AsteroidService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fabrick/v1.0")
@RequiredArgsConstructor
public class FabrickController {

    private final AsteroidService asteroidService;
    private final AirportStationService airportStationService;

    /**
     * Health check endpoint for application monitoring.
     * Returns application status, timestamp, and version information.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Fabrick Interview Exercise");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    /**
     * Get asteroid trajectory paths showing transitions between orbiting bodies.
     */
    @GetMapping("/asteroids/{asteroidId}/paths")
    public Mono<List<AsteroidPath>> getAsteroidPaths(
            @PathVariable String asteroidId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) {

        // Apply default date range if not specified
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusYears(100);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        return asteroidService.getAsteroidPaths(asteroidId, from, to);
    }

    /**
     * Find all weather stations within a bounding box around the specified airport.
     */
    @GetMapping("/airports/{airportId}/stations")
    public Mono<List<Station>> getClosestStations(
            @PathVariable String airportId,
            @RequestParam(defaultValue = "0.0") Double closestBy) {

        return airportStationService.getClosestStations(airportId, closestBy);
    }

    /**
     * Find all airports within a bounding box around the specified weather station.
     */
    @GetMapping("/stations/{stationId}/airports")
    public Mono<List<Airport>> getClosestAirports(
            @PathVariable String stationId,
            @RequestParam(defaultValue = "0.0") Double closestBy) {

        return airportStationService.getClosestAirports(stationId, closestBy);
    }
}