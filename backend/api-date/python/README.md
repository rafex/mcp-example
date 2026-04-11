# API Date Python

Backend REST autenticado en Python usando solo biblioteca estándar.

## Qué demuestra

Este ejemplo existe para mostrar un patrón común:

- el backend REST exige autenticación por headers
- el cliente humano o técnico podría llamarlo directo con `curl` o Postman
- el MCP puede colocarse delante para ocultar esos detalles al agente de IA

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

Valores por defecto si no defines variables:

- token: `dev-date-token`
- client id: `mcp-date-client`

## Ubicaciones soportadas

El servicio responde para 53 ubicaciones:

- 50 países relevantes
- 3 zonas horarias de México:
  - `mx-central`
  - `mx-northwest`
  - `mx-southeast`

La lista completa se obtiene desde `GET /date/locations`.

## Ejecutar

```bash
just run-python-api-date
```

O directamente:

```bash
python3 backend/api-date/python/server.py
```

## Probar con curl

```bash
curl "http://127.0.0.1:8090/date/time?location=jp" \
  -H "Authorization: Bearer dev-date-token" \
  -H "X-Date-Client: mcp-date-client"
```

```bash
curl "http://127.0.0.1:8090/date/locations" \
  -H "Authorization: Bearer dev-date-token" \
  -H "X-Date-Client: mcp-date-client"
```

## Logs

Los logs se escriben en:

- `logs/backend-api-date-python.log`
