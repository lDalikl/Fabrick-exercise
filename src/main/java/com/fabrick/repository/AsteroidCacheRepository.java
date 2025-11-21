package com.fabrick.repository;

import com.fabrick.entity.AsteroidCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsteroidCacheRepository extends JpaRepository<AsteroidCacheEntity, Long> {

}