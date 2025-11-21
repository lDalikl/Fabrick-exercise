package com.fabrick.repository;

import com.fabrick.entity.AirportCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportCacheRepository  extends JpaRepository<AirportCacheEntity, Long> {

    Optional<AirportCacheEntity> findByAirportId(String airportId);

    @Query("SELECT a FROM AirportCacheEntity a WHERE " +
            "a.latitude BETWEEN :minLat AND :maxLat AND " +
            "a.longitude BETWEEN :minLon AND :maxLon")
    List<AirportCacheEntity> findInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);
}
