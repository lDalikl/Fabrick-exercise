package com.fabrick.service;

import com.fabrick.repository.AirportCacheRepository;
import com.fabrick.repository.StationCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AirportDatabaseServiceTest {

    private AirportCacheRepository airportCacheRepository;
    private StationCacheRepository stationCacheRepository;
    private AirportDatabaseService airportDatabaseService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        airportCacheRepository = mock(AirportCacheRepository.class);
        stationCacheRepository = mock(StationCacheRepository.class);

        // Mock saveAll to return what was passed in
        when(airportCacheRepository.saveAll(anyList())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        when(stationCacheRepository.saveAll(anyList())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        airportDatabaseService = new AirportDatabaseService(
                airportCacheRepository,
                stationCacheRepository
        );
    }

    @Test
    void testLoadAirportsDatabase_FileNotFound() {
        // Act: call with non-existent file (will log warning but not throw)
        airportDatabaseService.loadAirportsDatabase();

        // Assert: should not attempt to save anything
        verify(airportCacheRepository, never()).saveAll(anyList());
        verify(stationCacheRepository, never()).saveAll(anyList());
    }
}