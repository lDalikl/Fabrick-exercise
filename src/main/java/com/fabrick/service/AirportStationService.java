package com.fabrick.service;

import com.fabrick.entity.AirportCacheEntity;
import com.fabrick.entity.StationCacheEntity;
import com.fabrick.model.Airport;
import com.fabrick.model.Station;
import com.fabrick.repository.AirportCacheRepository;
import com.fabrick.repository.StationCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirportStationService {

    private final WebClient aviationWebClient;
    private final AirportCacheRepository airportCacheRepository;
    private final StationCacheRepository stationCacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Find all stations within a bounding box around the specified airport.
     * Uses local database for airport lookup with external API fallback.
     */
    @Cacheable(value = "stations", key = "#airportId + '-' + #closestBy")
    public Mono<List<Station>> getClosestStations(String airportId, Double closestBy) {
        log.info("Fetching closest stations for airport: {}, closestBy: {}", airportId, closestBy);

        // Try local database first, then fall back to external API
        return Mono.fromCallable(() -> airportCacheRepository.findByAirportId(airportId))
                .flatMap(optionalAirport -> {
                    if (optionalAirport.isPresent()) {
                        AirportCacheEntity airportEntity = optionalAirport.get();
                        log.info("Found airport {} in local database at lat={}, lon={}",
                                airportId, airportEntity.getLatitude(), airportEntity.getLongitude());

                        Airport airport = Airport.builder()
                                .id(airportEntity.getAirportId())
                                .name(airportEntity.getName())
                                .state(airportEntity.getState())
                                .country(airportEntity.getCountry())
                                .latitude(airportEntity.getLatitude())
                                .longitude(airportEntity.getLongitude())
                                .elevation(airportEntity.getElevation())
                                .build();

                        return Mono.just(airport);
                    } else {
                        log.info("Airport {} not found in local database, trying external API", airportId);
                        return getAirportInfo(airportId);
                    }
                })
                .flatMap(airport -> {
                    if (airport == null || airport.getLatitude() == null || airport.getLongitude() == null) {
                        log.warn("Airport {} not found or has invalid coordinates", airportId);
                        return Mono.just(new ArrayList<Station>());
                    }

                    // Calculate bounding box coordinates
                    double minLat = airport.getLatitude() - closestBy;
                    double minLon = airport.getLongitude() - closestBy;
                    double maxLat = airport.getLatitude() + closestBy;
                    double maxLon = airport.getLongitude() + closestBy;

                    log.debug("Bounding box for {}: lat[{},{}] lon[{},{}]",
                            airportId, minLat, maxLat, minLon, maxLon);

                    return getStationsInBoundingBox(minLat, maxLat, minLon, maxLon);
                })
                .switchIfEmpty(Mono.just(new ArrayList<Station>()))
                .doOnError(error -> log.error("Error fetching stations for airport {}", airportId, error))
                .onErrorResume(error -> Mono.just(new ArrayList<Station>()));
    }

    /**
     * Find all airports within a bounding box around the specified station.
     * Uses local database for station lookup with external API fallback.
     */
    @Cacheable(value = "airports", key = "#stationId + '-' + #closestBy")
    public Mono<List<Airport>> getClosestAirports(String stationId, Double closestBy) {
        log.info("Fetching closest airports for station: {}, closestBy: {}", stationId, closestBy);

        // Try local database first, then fall back to external API
        return Mono.fromCallable(() -> stationCacheRepository.findByStationId(stationId))
                .flatMap(optionalStation -> {
                    if (optionalStation.isPresent()) {
                        StationCacheEntity stationEntity = optionalStation.get();
                        log.info("Found station {} in local database at lat={}, lon={}",
                                stationId, stationEntity.getLatitude(), stationEntity.getLongitude());

                        Station station = Station.builder()
                                .id(stationEntity.getStationId())
                                .site(stationEntity.getSite())
                                .state(stationEntity.getState())
                                .country(stationEntity.getCountry())
                                .latitude(stationEntity.getLatitude())
                                .longitude(stationEntity.getLongitude())
                                .elevation(stationEntity.getElevation())
                                .build();

                        return Mono.just(station);
                    } else {
                        log.info("Station {} not found in local database, trying external API", stationId);
                        return getStationInfo(stationId);
                    }
                })
                .flatMap(station -> {
                    if (station == null || station.getLatitude() == null || station.getLongitude() == null) {
                        log.warn("Station {} not found or has invalid coordinates", stationId);
                        return Mono.just(new ArrayList<Airport>());
                    }

                    // Calculate bounding box coordinates
                    double minLat = station.getLatitude() - closestBy;
                    double minLon = station.getLongitude() - closestBy;
                    double maxLat = station.getLatitude() + closestBy;
                    double maxLon = station.getLongitude() + closestBy;

                    log.debug("Bounding box for {}: lat[{},{}] lon[{},{}]",
                            stationId, minLat, maxLat, minLon, maxLon);

                    return getAirportsInBoundingBox(minLat, maxLat, minLon, maxLon);
                })
                .switchIfEmpty(Mono.just(Collections.emptyList()))
                .doOnError(error -> log.error("Error fetching airports for station {}", stationId, error))
                .onErrorReturn(new ArrayList<>());
    }

    /**
     * Fetch airport information from external Aviation Weather API.
     * Used as fallback when airport is not found in local database.
     */
    private Mono<Airport> getAirportInfo(String airportId) {
        log.debug("Fetching airport info from external API for: {}", airportId);

        return aviationWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/data/metar")
                        .queryParam("ids", airportId)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> parseAirportFromMetarJson(json, airportId))
                .filter(airport -> airport != null)
                .doOnNext(airport -> log.debug("Found airport from external API: {}", airport))
                .onErrorResume(error -> {
                    log.warn("Error retrieving airport info from external API for: {}", airportId, error);
                    return Mono.empty();
                });
    }

    /**
     * Fetch station information from external Aviation Weather API.
     * Used as fallback when station is not found in local database.
     */
    private Mono<Station> getStationInfo(String stationId) {
        log.debug("Fetching station info from external API for: {}", stationId);

        return aviationWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/data/metar")
                        .queryParam("ids", stationId)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> parseStationFromMetarJson(json, stationId))
                .doOnNext(station -> log.debug("Found station from external API: {}", station))
                .onErrorResume(error -> {
                    log.warn("Error retrieving station info from external API for: {}", stationId, error);
                    return Mono.empty();
                });
    }

    /**
     * Query local database for airports within specified geographic bounding box.
     */
    private Mono<List<Airport>> getAirportsInBoundingBox(double minLat, double maxLat,
                                                         double minLon, double maxLon) {
        log.debug("Searching airports in DB: lat[{},{}] lon[{},{}]", minLat, maxLat, minLon, maxLon);

        return Mono.fromCallable(() ->
                        airportCacheRepository.findInBoundingBox(minLat, maxLat, minLon, maxLon))
                .map(entities -> {
                    List<Airport> airports = new ArrayList<>();
                    for (AirportCacheEntity entity : entities) {
                        airports.add(Airport.builder()
                                .id(entity.getAirportId())
                                .name(entity.getName())
                                .state(entity.getState())
                                .country(entity.getCountry())
                                .latitude(entity.getLatitude())
                                .longitude(entity.getLongitude())
                                .elevation(entity.getElevation())
                                .build());
                    }
                    return airports;
                })
                .doOnNext(airports -> log.info("Found {} airports in bounding box", airports.size()))
                .defaultIfEmpty(new ArrayList<Airport>())
                .onErrorResume(error -> {
                    log.error("Error fetching airports from DB", error);
                    return Mono.just(new ArrayList<Airport>());
                });
    }

    /**
     * Query local database for stations within specified geographic bounding box.
     */
    private Mono<List<Station>> getStationsInBoundingBox(double minLat, double maxLat,
                                                         double minLon, double maxLon) {
        log.debug("Searching stations in DB: lat[{},{}] lon[{},{}]", minLat, maxLat, minLon, maxLon);

        return Mono.fromCallable(() -> {
                    List<StationCacheEntity> entities = stationCacheRepository.findInBoundingBox(minLat, maxLat, minLon, maxLon);
                    List<Station> stations = new ArrayList<>();

                    for (StationCacheEntity entity : entities) {
                        Station station = Station.builder()
                                .id(entity.getStationId())
                                .site(entity.getSite())
                                .state(entity.getState())
                                .country(entity.getCountry())
                                .latitude(entity.getLatitude())
                                .longitude(entity.getLongitude())
                                .elevation(entity.getElevation())
                                .build();
                        stations.add(station);
                    }

                    return stations;
                })
                .doOnNext(stations -> log.info("Found {} stations in bounding box", stations.size()))
                .defaultIfEmpty(new ArrayList<Station>())
                .onErrorResume(error -> {
                    log.error("Error fetching stations from DB", error);
                    return Mono.just(new ArrayList<Station>());
                });
    }

    /**
     * Parse airport data from METAR JSON response.
     */
    private Airport parseAirportFromMetarJson(String json, String airportId) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);

                return Airport.builder()
                        .id(first.path("icaoId").asText(airportId))
                        .name(first.path("name").asText(first.path("site").asText()))
                        .state(first.path("state").asText())
                        .country(first.path("country").asText())
                        .latitude(first.path("lat").asDouble())
                        .longitude(first.path("lon").asDouble())
                        .elevation(first.path("elev").asDouble())
                        .build();
            }

            log.warn("No data found for airport: {}", airportId);
            return null;

        } catch (Exception e) {
            log.error("Error parsing airport JSON for {}", airportId, e);
            return null;
        }
    }

    /**
     * Parse station data from METAR JSON response.
     */
    private Station parseStationFromMetarJson(String json, String stationId) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);

                return Station.builder()
                        .id(first.path("icaoId").asText(stationId))
                        .site(first.path("name").asText(first.path("site").asText()))
                        .state(first.path("state").asText())
                        .country(first.path("country").asText())
                        .latitude(first.path("lat").asDouble())
                        .longitude(first.path("lon").asDouble())
                        .elevation(first.path("elev").asInt())
                        .build();
            }

            log.warn("No data found for station: {}", stationId);
            return null;

        } catch (Exception e) {
            log.error("Error parsing station JSON for {}", stationId, e);
            return null;
        }
    }
}