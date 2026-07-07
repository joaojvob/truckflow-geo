package com.truckflow.geo.controller;

import com.truckflow.geo.dto.ApiResponse;
import com.truckflow.geo.dto.PlaceResult;
import com.truckflow.geo.service.GeoServiceException;
import com.truckflow.geo.service.GoogleMapsClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/places")
public class PlacesController {

    private final GoogleMapsClient googleMapsClient;

    public PlacesController(GoogleMapsClient googleMapsClient) {
        this.googleMapsClient = googleMapsClient;
    }

    @GetMapping("/nearby")
    public ApiResponse<List<Map<String, Object>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String type,
            @RequestParam(defaultValue = "5000") int radius
    ) {
        List<PlaceResult> places = googleMapsClient.searchNearby(lat, lng, type, radius);

        List<Map<String, Object>> data = new ArrayList<>();
        for (PlaceResult place : places) {
            Map<String, Object> item = new HashMap<>();
            item.put("place_id", place.placeId());
            item.put("name", place.name());
            item.put("address", place.address());
            item.put("lat", place.lat());
            item.put("lng", place.lng());
            item.put("rating", place.rating());
            item.put("open_now", place.openNow());
            data.add(item);
        }

        return ApiResponse.of(data);
    }

    @ExceptionHandler(GeoServiceException.class)
    public ResponseEntity<Map<String, String>> handleGeoError(GeoServiceException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("message", exception.getMessage()));
    }
}
