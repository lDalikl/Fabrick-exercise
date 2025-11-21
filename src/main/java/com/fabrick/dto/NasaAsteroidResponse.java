package com.fabrick.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NasaAsteroidResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("close_approach_data")
    private List<CloseApproachData> closeApproachData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CloseApproachData {

        @JsonProperty("close_approach_date")
        private String closeApproachDate;

        @JsonProperty("orbiting_body")
        private String orbitingBody;
    }
}