# API OpenWeather Java

Wrapper REST mínimo en Java sobre OpenWeatherMap.

## Qué hace

Expone los mismos endpoints conceptuales que la versión Python:

- `GET /openweather/current`
- `GET /openweather/overview`
- `GET /openweather/resources`
- `GET /openweather/resources/{name}`
- `GET /openweather/prompts`
- `GET /openweather/prompts/{name}`

## Variables de entorno

- `OPENWEATHER_API_KEY` obligatorio
- `OPENWEATHER_BASE_URL` opcional, default `https://api.openweathermap.org`
- `OPENWEATHER_API_PORT` opcional, default `8101`

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key just run-java-api-openweather
```
