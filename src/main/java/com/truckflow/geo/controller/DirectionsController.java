package com.truckflow.geo.controller;

import com.truckflow.geo.dto.ApiResponse;
import com.truckflow.geo.dto.DirectionsRequest;
import com.truckflow.geo.dto.DirectionsResponse;
import com.truckflow.geo.service.GeoServiceException;
import com.truckflow.geo.service.GoogleMapsClient;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DirectionsController {

    private final GoogleMapsClient googleMapsClient;

    public DirectionsController(GoogleMapsClient googleMapsClient) {
        this.googleMapsClient = googleMapsClient;
    }

    @PostMapping("/directions")
    public ApiResponse<Map<String, Object>> directions(@Valid @RequestBody DirectionsRequest request) {
        DirectionsResponse route = googleMapsClient.getDirections(request);

        Map<String, Object> data = new HashMap<>();
        data.put("polyline", route.polyline());
        data.put("distance_meters", route.distanceMeters());
        data.put("duration_seconds", route.durationSeconds());
        data.put("bounds", route.bounds());

        return ApiResponse.of(data);
    }

    @ExceptionHandler(GeoServiceException.class)
    public ResponseEntity<Map<String, String>> handleGeoError(GeoServiceException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("message", exception.getMessage()));
    }
}
