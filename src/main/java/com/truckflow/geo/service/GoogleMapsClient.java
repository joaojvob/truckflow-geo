package com.truckflow.geo.service;

import com.truckflow.geo.dto.DirectionsRequest;
import com.truckflow.geo.dto.DirectionsResponse;
import com.truckflow.geo.dto.LatLng;
import com.truckflow.geo.dto.PlaceResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleMapsClient {

    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String PLACES_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    private final RestClient restClient;
    private final String apiKey;

    public GoogleMapsClient(
            RestClient.Builder restClientBuilder,
            @Value("${google.maps.api-key:}") String apiKey
    ) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    public DirectionsResponse getDirections(DirectionsRequest request) {
        ensureApiKey();

        var params = new java.util.LinkedHashMap<String, String>();
        params.put("origin", formatPoint(request.origin()));
        params.put("destination", formatPoint(request.destination()));
        params.put("key", apiKey);
        params.put("language", "pt-BR");
        params.put("region", "br");

        if (!request.waypoints().isEmpty()) {
            params.put("waypoints", request.waypoints().stream()
                    .map(this::formatPoint)
                    .collect(Collectors.joining("|")));
        }

        Map<String, Object> body = get(DIRECTIONS_URL, params);
        String status = String.valueOf(body.getOrDefault("status", "UNKNOWN"));

        if (!"OK".equals(status)) {
            throw new GeoServiceException(translateDirectionsStatus(status));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new GeoServiceException("Nenhuma rota encontrada.");
        }

        Map<String, Object> route = routes.getFirst();
        @SuppressWarnings("unchecked")
        Map<String, Object> polyline = (Map<String, Object>) route.get("overview_polyline");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");

        int distance = sumLegValue(legs, "distance");
        int duration = sumLegValue(legs, "duration");

        @SuppressWarnings("unchecked")
        Map<String, Object> bounds = (Map<String, Object>) route.get("bounds");

        return new DirectionsResponse(
                polyline != null ? String.valueOf(polyline.get("points")) : "",
                distance,
                duration,
                bounds
        );
    }

    public List<PlaceResult> searchNearby(double lat, double lng, String type, int radius) {
        ensureApiKey();

        Map<String, Object> body = get(PLACES_URL, Map.of(
                "location", lat + "," + lng,
                "radius", String.valueOf(radius),
                "type", type,
                "key", apiKey,
                "language", "pt-BR"
        ));

        String status = String.valueOf(body.getOrDefault("status", "UNKNOWN"));
        if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
            throw new GeoServiceException(translatePlacesStatus(status));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) body.getOrDefault("results", List.of());

        List<PlaceResult> places = new ArrayList<>();
        for (Map<String, Object> place : results.stream().limit(20).toList()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) place.get("geometry");
            @SuppressWarnings("unchecked")
            Map<String, Object> location = geometry != null
                    ? (Map<String, Object>) geometry.get("location")
                    : Map.of();

            Boolean openNow = null;
            if (place.get("opening_hours") instanceof Map<?, ?> hours) {
                openNow = (Boolean) hours.get("open_now");
            }

            places.add(new PlaceResult(
                    String.valueOf(place.getOrDefault("place_id", "")),
                    String.valueOf(place.getOrDefault("name", "Sem nome")),
                    place.get("vicinity") != null ? String.valueOf(place.get("vicinity")) : null,
                    location.get("lat") != null ? ((Number) location.get("lat")).doubleValue() : 0,
                    location.get("lng") != null ? ((Number) location.get("lng")).doubleValue() : 0,
                    place.get("rating") != null ? ((Number) place.get("rating")).doubleValue() : null,
                    openNow
            ));
        }

        return places;
    }

    private Map<String, Object> get(String url, Map<String, String> params) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            params.forEach(builder::queryParam);
            URI uri = builder.build().toUri();

            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            throw new GeoServiceException("Falha ao consultar Google Maps API.", exception);
        }
    }

    private int sumLegValue(List<Map<String, Object>> legs, String key) {
        if (legs == null) {
            return 0;
        }
        return legs.stream()
                .mapToInt(leg -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metric = (Map<String, Object>) leg.get(key);
                    return metric != null && metric.get("value") != null
                            ? ((Number) metric.get("value")).intValue()
                            : 0;
                })
                .sum();
    }

    private String formatPoint(LatLng point) {
        return point.lat() + "," + point.lng();
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeoServiceException("GOOGLE_MAPS_API_KEY não configurada.");
        }
    }

    private String translateDirectionsStatus(String status) {
        return switch (status) {
            case "ZERO_RESULTS" -> "Nenhuma rota encontrada entre origem e destino.";
            case "NOT_FOUND" -> "Origem ou destino não encontrados.";
            case "OVER_QUERY_LIMIT" -> "Limite de requisições da Google Maps API excedido.";
            case "REQUEST_DENIED" -> "Requisição negada pela Google Maps API.";
            case "INVALID_REQUEST" -> "Requisição inválida para a Google Directions API.";
            default -> "Erro ao calcular a rota na Google Maps API.";
        };
    }

    private String translatePlacesStatus(String status) {
        return switch (status) {
            case "OVER_QUERY_LIMIT" -> "Limite de requisições da Google Places API excedido.";
            case "REQUEST_DENIED" -> "Requisição negada pela Google Places API.";
            case "INVALID_REQUEST" -> "Requisição inválida para a Google Places API.";
            default -> "Erro ao buscar locais na Google Places API.";
        };
    }
}
