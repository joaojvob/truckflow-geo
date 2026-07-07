package com.truckflow.geo.dto;

import jakarta.validation.constraints.NotNull;

public record LatLng(
        @NotNull Double lat,
        @NotNull Double lng
) {}
