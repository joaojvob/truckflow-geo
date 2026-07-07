package com.truckflow.geo.dto;

public record PlaceResult(
        String placeId,
        String name,
        String address,
        double lat,
        double lng,
        Double rating,
        Boolean openNow
) {}
