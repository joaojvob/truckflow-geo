package com.truckflow.geo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DirectionsRequest(
        @NotNull @Valid LatLng origin,
        @NotNull @Valid LatLng destination,
        List<@Valid LatLng> waypoints
) {
    public DirectionsRequest {
        if (waypoints == null) {
            waypoints = List.of();
        }
    }
}
