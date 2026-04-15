# MCP OpenWeather Python

Servidor MCP manual en Python que habla directo con OpenWeatherMap.

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

- `OPENWEATHER_API_KEY` obligatorio
- `OPENWEATHER_BASE_URL` opcional, default `https://api.openweathermap.org`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-python-mcp-openweather
```
