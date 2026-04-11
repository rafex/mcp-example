# API Date Java

Backend REST autenticado en Java usando solo biblioteca estándar.

## Qué demuestra

Este ejemplo replica en Java el mismo caso de estudio de Python:

- el backend exige autenticación por headers
- la lógica de negocio se mantiene pequeña y explícita
- el MCP puede envolver esta API y ocultar credenciales operativas al agente

## Endpoints

- `GET /date/time?location=mx-central`
- `GET /date/locations`
- `GET /date/resources`
- `GET /date/resources/{name}`
- `GET /date/prompts`
- `GET /date/prompts/{name}`
- `OPTIONS` para todas esas rutas

## Autenticación

Headers requeridos:

- `Authorization: Bearer <token>`
- `X-Date-Client: <client-id>`

Valores por defecto:

- token: `dev-date-token`
- client id: `mcp-date-client`

## Ubicaciones soportadas

El servicio responde para 53 ubicaciones:

- 50 países relevantes
- 3 zonas horarias de México: `mx-central`, `mx-northwest`, `mx-southeast`

La lista completa se obtiene desde `GET /date/locations`.

## Ejecutar

```bash
just run-java-api-date
```

## Logs

Los logs se escriben en:

- `logs/backend-api-date-java.log`
