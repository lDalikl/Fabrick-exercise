package com.fabrick.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsteroidPath {

    @JsonProperty("fromPlanet")
    private String fromPlanet;

    @JsonProperty("toPlanet")
    private String toPlanet;

    @JsonProperty("fromDate")
    private String fromDate;

    @JsonProperty("toDate")
    private String toDate;
}
