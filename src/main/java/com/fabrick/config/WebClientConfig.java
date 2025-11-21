package com.fabrick.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient nasaWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.nasa.gov/neo/rest/v1")
                .defaultHeader("User-Agent", "Fabrick-Interview-Exercise/1.0")
                .build();
    }

    @Bean
    public WebClient aviationWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .baseUrl("https://aviationweather.gov")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Fabrick-Interview-Exercise/1.0")
                .build();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "asteroids", "airports", "stations"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}