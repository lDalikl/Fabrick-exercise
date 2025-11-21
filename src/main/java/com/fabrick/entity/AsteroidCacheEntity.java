package com.fabrick.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "asteroid_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsteroidCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String asteroidId;

    @Column(nullable = false)
    private String fromPlanet;

    @Column(nullable = false)
    private String toPlanet;

    @Column(nullable = false)
    private String fromDate;

    @Column(nullable = false)
    private String toDate;

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @PrePersist
    protected void onCreate() {
        cachedAt = LocalDateTime.now();
    }
}