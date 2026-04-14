# API OpenWeather Python

Wrapper REST mínimo en Python sobre OpenWeatherMap.

## Qué hace

Expone:

- `GET /openweather/current`
- `GET /openweather/overview`
- `GET /openweather/resources`
- `GET /openweather/resources/{name}`
- `GET /openweather/prompts`
- `GET /openweather/prompts/{name}`

Internamente consume:

- Current Weather API 2.5
- One Call API 3.0 overview
- Geocoding API para resolver `q`

## Variables de entorno

- `OPENWEATHER_API_KEY` obligatorio
- `OPENWEATHER_BASE_URL` opcional, default `https://api.openweathermap.org`
- `OPENWEATHER_API_PORT` opcional, default `8100`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-python-api-openweather
```
