package com.truckflow.geo.dto;

import java.util.Map;

public record DirectionsResponse(
        String polyline,
        int distanceMeters,
        int durationSeconds,
        Map<String, Object> bounds
) {}
