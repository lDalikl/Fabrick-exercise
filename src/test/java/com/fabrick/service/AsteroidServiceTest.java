package com.fabrick.service;

import com.fabrick.model.AsteroidPath;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidServiceTest {

    private MockWebServer mockWebServer;
    private AsteroidService asteroidService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        asteroidService = new AsteroidService(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetAsteroidPaths_Success() {
        String mockResponse = "{\n" +
                "    \"id\": \"3542519\",\n" +
                "    \"name\": \"Test Asteroid\",\n" +
                "    \"close_approach_data\": [\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1917-04-30\",\n" +
                "            \"orbiting_body\": \"Juptr\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1920-05-01\",\n" +
                "            \"orbiting_body\": \"Juptr\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1930-06-01\",\n" +
                "            \"orbiting_body\": \"Earth\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1950-08-07\",\n" +
                "            \"orbiting_body\": \"Juptr\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody(mockResponse);
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        LocalDate fromDate = LocalDate.of(1900, 1, 1);
        LocalDate toDate = LocalDate.of(2000, 12, 31);

        Mono<List<AsteroidPath>> result = asteroidService.getAsteroidPaths("3542519", fromDate, toDate);

        StepVerifier.create(result)
                .assertNext(paths -> {
                    assertEquals(2, paths.size());

                    AsteroidPath firstPath = paths.get(0);
                    assertEquals("Juptr", firstPath.getFromPlanet());
                    assertEquals("Earth", firstPath.getToPlanet());
                    assertEquals("1917-04-30", firstPath.getFromDate());
                    assertEquals("1930-06-01", firstPath.getToDate());

                    AsteroidPath secondPath = paths.get(1);
                    assertEquals("Earth", secondPath.getFromPlanet());
                    assertEquals("Juptr", secondPath.getToPlanet());
                    assertEquals("1930-06-01", secondPath.getFromDate());
                    assertEquals("1950-08-07", secondPath.getToDate());
                })
                .verifyComplete();
    }

    @Test
    void testGetAsteroidPaths_EmptyData() {
        String mockResponse = "{\n" +
                "    \"id\": \"3542519\",\n" +
                "    \"name\": \"Test Asteroid\",\n" +
                "    \"close_approach_data\": []\n" +
                "}";

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody(mockResponse);
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        LocalDate fromDate = LocalDate.of(1900, 1, 1);
        LocalDate toDate = LocalDate.of(2000, 12, 31);

        Mono<List<AsteroidPath>> result = asteroidService.getAsteroidPaths("3542519", fromDate, toDate);

        StepVerifier.create(result)
                .assertNext(paths -> {
                    assertTrue(paths.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void testGetAsteroidPaths_FiltersByDateRange() {
        String mockResponse = "{\n" +
                "    \"id\": \"3542519\",\n" +
                "    \"name\": \"Test Asteroid\",\n" +
                "    \"close_approach_data\": [\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1900-04-30\",\n" +
                "            \"orbiting_body\": \"Juptr\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"close_approach_date\": \"1930-06-01\",\n" +
                "            \"orbiting_body\": \"Earth\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"close_approach_date\": \"2100-08-07\",\n" +
                "            \"orbiting_body\": \"Juptr\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setBody(mockResponse);
        mockHttpResponse.addHeader("Content-Type", "application/json");
        mockWebServer.enqueue(mockHttpResponse);

        LocalDate fromDate = LocalDate.of(1920, 1, 1);
        LocalDate toDate = LocalDate.of(1950, 12, 31);

        Mono<List<AsteroidPath>> result = asteroidService.getAsteroidPaths("3542519", fromDate, toDate);

        StepVerifier.create(result)
                .assertNext(paths -> {
                    // Solo l'entry del 1930 dovrebbe essere incluso
                    boolean isValid = true;
                    for (AsteroidPath path : paths) {
                        LocalDate pathFromDate = LocalDate.parse(path.getFromDate());
                        LocalDate pathToDate = LocalDate.parse(path.getToDate());
                        boolean isAfterFromDate = pathFromDate.isAfter(fromDate.minusDays(1));
                        boolean isBeforeToDate = pathToDate.isBefore(toDate.plusDays(1));

                        if (!isAfterFromDate || !isBeforeToDate) {
                            isValid = false;
                            break;
                        }
                    }
                    assertTrue(paths.isEmpty() || isValid);
                })
                .verifyComplete();
    }

    @Test
    void testGetAsteroidPaths_ErrorHandling() {
        MockResponse mockHttpResponse = new MockResponse();
        mockHttpResponse.setResponseCode(404);
        mockWebServer.enqueue(mockHttpResponse);

        LocalDate fromDate = LocalDate.of(1900, 1, 1);
        LocalDate toDate = LocalDate.of(2000, 12, 31);

        Mono<List<AsteroidPath>> result = asteroidService.getAsteroidPaths("invalid", fromDate, toDate);

        StepVerifier.create(result)
                .assertNext(paths -> {
                    assertTrue(paths.isEmpty());
                })
                .verifyComplete();
    }
}