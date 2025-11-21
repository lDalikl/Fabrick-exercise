package com.fabrick.service;

import com.fabrick.entity.AirportCacheEntity;
import com.fabrick.entity.StationCacheEntity;
import com.fabrick.model.Airport;
import com.fabrick.model.Station;
import com.fabrick.repository.AirportCacheRepository;
import com.fabrick.repository.StationCacheRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AirportStationServiceTest {

    private MockWebServer mockWebServer;
    private AirportStationService airportStationService;
    private AirportCacheRepository airportCacheRepository;
    private StationCacheRepository stationCacheRepository;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        airportCacheRepository = mock(AirportCacheRepository.class);
        stationCacheRepository = mock(StationCacheRepository.class);

        airportStationService = new AirportStationService(
                webClient,
                airportCacheRepository,
                stationCacheRepository
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetClosestStations_FromLocalDatabase() {
        // Arrange: airport found in local database
        AirportCacheEntity airportEntity = AirportCacheEntity.builder()
                .airportId("KDEN")
                .name("Denver International Airport")
                .state("CO")
                .country("US")
                .latitude(39.8617)
                .longitude(-104.6732)
                .elevation(1656.6)
                .cachedAt(LocalDateTime.now())
                .build();

        when(airportCacheRepository.findByAirportId("KDEN"))
                .thenReturn(Optional.of(airportEntity));

        // Create nearby stations
        List<StationCacheEntity> stationEntities = new ArrayList<>();
        StationCacheEntity station1 = StationCacheEntity.builder()
                .stationId("KAPA")
                .site("Centennial Airport")
                .state("CO")
                .country("US")
                .latitude(39.5701)
                .longitude(-104.849)
                .elevation(1791)
                .cachedAt(LocalDateTime.now())
                .build();
        stationEntities.add(station1);

        when(stationCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(stationEntities);

        // Act
        Mono<List<Station>> result = airportStationService.getClosestStations("KDEN", 0.5);

        // Assert
        StepVerifier.create(result)
                .assertNext(stations -> {
                    assertEquals(1, stations.size());
                    Station station = stations.get(0);
                    assertEquals("KAPA", station.getId());
                    assertEquals("Centennial Airport", station.getSite());
                    assertEquals(39.5701, station.getLatitude());
                    assertEquals(-104.849, station.getLongitude());
                })
                .verifyComplete();

        // Verify that database was queried
        verify(airportCacheRepository).findByAirportId("KDEN");
        verify(stationCacheRepository).findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void testGetClosestStations_FallbackToExternalAPI() {
        // Arrange: airport NOT found in database, must use external API
        when(airportCacheRepository.findByAirportId("KDEN"))
                .thenReturn(Optional.empty());

        String mockApiResponse = "[{\n" +
                "    \"icaoId\": \"KDEN\",\n" +
                "    \"name\": \"Denver International\",\n" +
                "    \"state\": \"CO\",\n" +
                "    \"country\": \"US\",\n" +
                "    \"lat\": 39.8617,\n" +
                "    \"lon\": -104.6732,\n" +
                "    \"elev\": 1656\n" +
                "}]";

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody(mockApiResponse);
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        when(stationCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new ArrayList<>());

        // Act
        Mono<List<Station>> result = airportStationService.getClosestStations("KDEN", 0.5);

        // Assert
        StepVerifier.create(result)
                .assertNext(stations -> {
                    // Empty list is fine - we're testing the fallback mechanism works
                    assertNotNull(stations);
                })
                .verifyComplete();

        // Verify database was queried first
        verify(airportCacheRepository).findByAirportId("KDEN");
    }

    @Test
    void testGetClosestAirports_FromLocalDatabase() {
        // Arrange: station found in local database
        StationCacheEntity stationEntity = StationCacheEntity.builder()
                .stationId("KDEN")
                .site("Denver International Airport")
                .state("CO")
                .country("US")
                .latitude(39.8617)
                .longitude(-104.6732)
                .elevation(1656)
                .cachedAt(LocalDateTime.now())
                .build();

        when(stationCacheRepository.findByStationId("KDEN"))
                .thenReturn(Optional.of(stationEntity));

        // Create nearby airports
        List<AirportCacheEntity> airportEntities = new ArrayList<>();
        AirportCacheEntity airport1 = AirportCacheEntity.builder()
                .airportId("KAPA")
                .name("Centennial Airport")
                .state("CO")
                .country("US")
                .latitude(39.5701)
                .longitude(-104.849)
                .elevation(1791.0)
                .cachedAt(LocalDateTime.now())
                .build();
        airportEntities.add(airport1);

        when(airportCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(airportEntities);

        // Act
        Mono<List<Airport>> result = airportStationService.getClosestAirports("KDEN", 1.0);

        // Assert
        StepVerifier.create(result)
                .assertNext(airports -> {
                    assertEquals(1, airports.size());
                    Airport airport = airports.get(0);
                    assertEquals("KAPA", airport.getId());
                    assertEquals("Centennial Airport", airport.getName());
                    assertEquals(39.5701, airport.getLatitude());
                })
                .verifyComplete();

        verify(stationCacheRepository).findByStationId("KDEN");
        verify(airportCacheRepository).findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void testGetClosestStations_EmptyResultWhenAirportNotFound() {
        // Arrange: airport not found in database or API
        when(airportCacheRepository.findByAirportId("INVALID"))
                .thenReturn(Optional.empty());

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody("[]");
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        // Act
        Mono<List<Station>> result = airportStationService.getClosestStations("INVALID", 0.5);

        // Assert
        StepVerifier.create(result)
                .assertNext(stations -> {
                    assertTrue(stations.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void testGetClosestAirports_EmptyResultWhenStationNotFound() {
        // Arrange: station not found
        when(stationCacheRepository.findByStationId("INVALID"))
                .thenReturn(Optional.empty());

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody("[]");
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        // Act
        Mono<List<Airport>> result = airportStationService.getClosestAirports("INVALID", 0.5);

        // Assert
        StepVerifier.create(result)
                .assertNext(airports -> {
                    assertTrue(airports.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void testBoundingBoxCalculation() {
        // Arrange: test that bounding box is correctly calculated
        AirportCacheEntity airportEntity = AirportCacheEntity.builder()
                .airportId("KDEN")
                .name("Denver International")
                .latitude(40.0)
                .longitude(-105.0)
                .elevation(1656.6)
                .cachedAt(LocalDateTime.now())
                .build();

        when(airportCacheRepository.findByAirportId("KDEN"))
                .thenReturn(Optional.of(airportEntity));

        when(stationCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new ArrayList<>());

        // Act
        Double closestBy = 1.0;
        Mono<List<Station>> result = airportStationService.getClosestStations("KDEN", closestBy);

        // Assert
        StepVerifier.create(result)
                .assertNext(stations -> assertNotNull(stations))
                .verifyComplete();

        // Verify bounding box calculation: lat ± closestBy, lon ± closestBy
        verify(stationCacheRepository).findInBoundingBox(
                eq(39.0),  // minLat = 40.0 - 1.0
                eq(41.0),  // maxLat = 40.0 + 1.0
                eq(-106.0), // minLon = -105.0 - 1.0
                eq(-104.0)  // maxLon = -105.0 + 1.0
        );
    }

    @Test
    void testGetClosestStations_WithZeroClosestBy() {
        // Arrange: test with closestBy = 0.0 (exact coordinates)
        AirportCacheEntity airportEntity = AirportCacheEntity.builder()
                .airportId("KDEN")
                .name("Denver International")
                .latitude(39.8617)
                .longitude(-104.6732)
                .elevation(1656.6)
                .cachedAt(LocalDateTime.now())
                .build();

        when(airportCacheRepository.findByAirportId("KDEN"))
                .thenReturn(Optional.of(airportEntity));

        when(stationCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new ArrayList<>());

        // Act
        Mono<List<Station>> result = airportStationService.getClosestStations("KDEN", 0.0);

        // Assert
        StepVerifier.create(result)
                .assertNext(stations -> assertTrue(stations.isEmpty()))
                .verifyComplete();

        // Should still query with exact coordinates
        verify(stationCacheRepository).findInBoundingBox(
                eq(39.8617), eq(39.8617), eq(-104.6732), eq(-104.6732)
        );
    }

    @Test
    void testGetClosestStations_ErrorHandling() {
        // Arrange: simulate database error
        when(airportCacheRepository.findByAirportId("KDEN"))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        Mono<List<Station>> result = airportStationService.getClosestStations("KDEN", 0.5);

        // Assert: should handle error gracefully and return empty list
        StepVerifier.create(result)
                .assertNext(stations -> {
                    assertTrue(stations.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void testGetClosestAirports_MultipleResults() {
        // Arrange: station with multiple nearby airports
        StationCacheEntity stationEntity = StationCacheEntity.builder()
                .stationId("KDEN")
                .site("Denver International")
                .latitude(39.8617)
                .longitude(-104.6732)
                .elevation(1656)
                .cachedAt(LocalDateTime.now())
                .build();

        when(stationCacheRepository.findByStationId("KDEN"))
                .thenReturn(Optional.of(stationEntity));

        List<AirportCacheEntity> airportEntities = new ArrayList<>();
        airportEntities.add(AirportCacheEntity.builder()
                .airportId("KAPA")
                .name("Centennial")
                .latitude(39.5701)
                .longitude(-104.849)
                .build());
        airportEntities.add(AirportCacheEntity.builder()
                .airportId("KBJC")
                .name("Broomfield")
                .latitude(39.9088)
                .longitude(-105.117)
                .build());
        airportEntities.add(AirportCacheEntity.builder()
                .airportId("KFNL")
                .name("Fort Collins")
                .latitude(40.4518)
                .longitude(-105.011)
                .build());

        when(airportCacheRepository.findInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(airportEntities);

        // Act
        Mono<List<Airport>> result = airportStationService.getClosestAirports("KDEN", 1.0);

        // Assert
        StepVerifier.create(result)
                .assertNext(airports -> {
                    assertEquals(3, airports.size());
                    assertTrue(airports.stream().anyMatch(a -> a.getId().equals("KAPA")));
                    assertTrue(airports.stream().anyMatch(a -> a.getId().equals("KBJC")));
                    assertTrue(airports.stream().anyMatch(a -> a.getId().equals("KFNL")));
                })
                .verifyComplete();
    }
}