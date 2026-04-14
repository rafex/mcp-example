# MCP OpenWeather FastMCP Python

Servidor MCP `openweather` en Python usando FastMCP, el SDK oficial de MCP para Python.

Este ejemplo es deliberadamente distinto a `hello` y `date`: aquí el repositorio muestra la variante basada en SDK en lugar de una implementación manual.

## Qué expone

### Tools

- `get_current_weather`
- `get_weather_overview`

### Resources

- `openweather://service-overview`
- `openweather://unit-reference`

### Prompts

FastMCP publica estos prompts desde funciones Python:

- `current_weather_brief`
- `weather_overview_brief`

## Dependencia

FastMCP forma parte del paquete oficial `mcp` para Python:

```bash
python3 -m pip install -r mcp-server/openweather/python/requirements.txt
```

## Variables de entorno

- `OPENWEATHER_API_BASE_URL` opcional, default `http://127.0.0.1:8100`

## Arquitectura

El servidor MCP no llama OpenWeatherMap directamente.

Actúa como wrapper del backend REST `api-openweather`:

- `get_current_weather` llama `GET /openweather/current`
- `get_weather_overview` llama `GET /openweather/overview`
- los resources leen `GET /openweather/resources/...`
- los prompts reutilizan `GET /openweather/prompts/...`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-python-api-openweather
OPENWEATHER_API_BASE_URL=http://127.0.0.1:8100 just run-python-mcp-openweather
```
