package com.fabrick.repository;

import com.fabrick.entity.StationCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationCacheRepository  extends JpaRepository<StationCacheEntity, Long> {

    Optional<StationCacheEntity> findByStationId(String stationId);

    @Query("SELECT s FROM StationCacheEntity s WHERE " +
            "s.latitude BETWEEN :minLat AND :maxLat AND " +
            "s.longitude BETWEEN :minLon AND :maxLon")
    List<StationCacheEntity> findInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);
}
