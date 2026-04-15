# MCP OpenWeather FastMCP Python

Servidor MCP `openweather-fastmcp` en Python usando FastMCP, el SDK oficial de MCP para Python, hablando directo con OpenWeatherMap.

Este ejemplo existe para contrastar con `mcp-server/openweather/python`, que implementa MCP manualmente sobre `stdio`.

## Qué expone

### Tools

- `get_current_weather`
- `get_weather_overview`

### Resources

- `openweather://service-overview`
- `openweather://unit-reference`

### Prompts

- `current_weather_brief`
- `weather_overview_brief`

## Dependencia

```bash
python3 -m pip install -r mcp-server/openweather-fastmcp/python/requirements.txt
```

## Ejemplo

```bash
OPENWEATHER_API_KEY=tu_api_key OPENWEATHER_BASE_URL=https://api.openweathermap.org just run-python-mcp-openweather-fastmcp
```
