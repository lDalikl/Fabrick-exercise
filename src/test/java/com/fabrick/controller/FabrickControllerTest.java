package com.fabrick.controller;

import com.fabrick.model.Airport;
import com.fabrick.model.AsteroidPath;
import com.fabrick.model.Station;
import com.fabrick.service.AirportStationService;
import com.fabrick.service.AsteroidService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(FabrickController.class)
class FabrickControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AsteroidService asteroidService;

    @MockBean
    private AirportStationService airportStationService;

    @Test
    void testGetAsteroidPaths_WithAllParameters() {
        // Creare il percorso dell'asteroide
        AsteroidPath path = new AsteroidPath();
        path.setFromPlanet("Earth");
        path.setToPlanet("Mars");
        path.setFromDate("2020-01-01");
        path.setToDate("2020-06-01");

        // Creare la lista con il percorso
        List<AsteroidPath> pathList = new ArrayList<>();
        pathList.add(path);

        when(asteroidService.getAsteroidPaths(
                eq("3542519"),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Mono.just(pathList));

        webTestClient.get()
                .uri("/api/fabrick/v1.0/asteroids/3542519/paths?fromDate=2020-01-01&toDate=2020-12-31")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AsteroidPath.class)
                .hasSize(1)
                .contains(path);
    }

    @Test
    void testGetAsteroidPaths_WithDefaultDates() {
        // Creare una lista vuota
        List<AsteroidPath> emptyPathList = new ArrayList<>();

        when(asteroidService.getAsteroidPaths(
                eq("3542519"),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Mono.just(emptyPathList));

        webTestClient.get()
                .uri("/api/fabrick/v1.0/asteroids/3542519/paths")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AsteroidPath.class)
                .hasSize(0);
    }

    @Test
    void testGetClosestStations_WithDefaultClosestBy() {
        // Creare una lista vuota
        List<Station> emptyStationList = new ArrayList<>();

        when(airportStationService.getClosestStations(eq("KDEN"), eq(0.0)))
                .thenReturn(Mono.just(emptyStationList));

        webTestClient.get()
                .uri("/api/fabrick/v1.0/airports/KDEN/stations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Station.class)
                .hasSize(0);
    }

    @Test
    void testGetClosestStations_WithCustomClosestBy() {
        // Creare una lista vuota
        List<Station> emptyStationList = new ArrayList<>();

        when(airportStationService.getClosestStations(eq("KDEN"), eq(1.0)))
                .thenReturn(Mono.just(emptyStationList));

        webTestClient.get()
                .uri("/api/fabrick/v1.0/airports/KDEN/stations?closestBy=1.0")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testGetClosestAirports_Success() {
        // Creare l'aeroporto
        Airport airport = new Airport();
        airport.setId("KDEN");
        airport.setName("Denver International");
        airport.setState("CO");
        airport.setCountry("US");
        airport.setLatitude(39.8617);
        airport.setLongitude(-104.6732);
        airport.setElevation(1656.6);

        // Creare la lista con l'aeroporto
        List<Airport> airportList = new ArrayList<>();
        airportList.add(airport);

        when(airportStationService.getClosestAirports(eq("KAFF"), eq(0.5)))
                .thenReturn(Mono.just(airportList));

        webTestClient.get()
                .uri("/api/fabrick/v1.0/stations/KAFF/airports?closestBy=0.5")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Airport.class)
                .hasSize(1)
                .contains(airport);
    }
}