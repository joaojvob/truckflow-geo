# TruckFlow Geo — microserviço de geolocalização

Serviço **Spring Boot** que encapsula Google Directions e Places API. Integra com `truckflow-api` via interface `RoutingProvider` quando `GEO_ROUTING_DRIVER=java`.

## Stack

- Java 21
- Spring Boot 3.4
- Spring Web
- Resilience4j (circuit breaker — preparado para evolução)

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/actuator/health` | Health check |
| POST | `/api/v1/directions` | Calcula rota (polyline, distância, duração) |
| GET | `/api/v1/places/nearby` | Busca POIs próximos |

## Configuração

```yaml
# application.yml
google.maps.api-key: ${GOOGLE_MAPS_API_KEY}
server.port: 8081
```

## Rodar localmente

```bash
# Com Maven instalado
export GOOGLE_MAPS_API_KEY=sua-chave
./mvnw spring-boot:run

# Ou via Docker
docker build -t truckflow-geo .
docker run -p 8081:8081 -e GOOGLE_MAPS_API_KEY=sua-chave truckflow-geo
```

## Integração com Laravel

No `.env` do `truckflow-api`:

```env
GEO_ROUTING_DRIVER=java
GEO_JAVA_SERVICE_URL=http://localhost:8081
```

## Exemplo — Directions

```bash
curl -X POST http://localhost:8081/api/v1/directions \
  -H 'Content-Type: application/json' \
  -d '{
    "origin": {"lat": -23.5505, "lng": -46.6333},
    "destination": {"lat": -22.9068, "lng": -43.1729},
    "waypoints": []
  }'
```

## Arquitetura

```
DirectionsController / PlacesController
        ↓
GoogleMapsClient (HTTP → Google APIs)
        ↓
DTOs de request/response (contrato estável com Laravel)
```

## Licença

Proprietary — parte do ecossistema TruckFlow.
