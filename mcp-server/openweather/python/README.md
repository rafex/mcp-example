# MCP OpenWeather Python

Servidor MCP en Python que envuelve el backend REST `api-openweather` Python.

## Tools

- `get_current_weather`
- `get_weather_overview`

## Resources

- `openweather://service-overview`
- `openweather://unit-reference`

## Prompts

- `current-weather-brief`
- `weather-overview-brief`

## Variables de entorno

- `OPENWEATHER_API_BASE_URL` opcional, default `http://127.0.0.1:8100`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-python-api-openweather
OPENWEATHER_API_BASE_URL=http://127.0.0.1:8100 just run-python-mcp-openweather
```
