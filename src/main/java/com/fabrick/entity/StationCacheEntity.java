package com.fabrick.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "station_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String stationId;

    private String site;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;
    private Integer elevation;

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @PrePersist
    protected void onCreate() {
        cachedAt = LocalDateTime.now();
    }
}
