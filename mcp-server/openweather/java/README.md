# MCP OpenWeather Java

Servidor MCP manual en Java que envuelve el backend REST `api-openweather` Java.

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

- `OPENWEATHER_API_BASE_URL` opcional, default `http://127.0.0.1:8101`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-java-api-openweather
OPENWEATHER_API_BASE_URL=http://127.0.0.1:8101 just run-java-mcp-openweather
```
