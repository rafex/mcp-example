# MCP OpenWeather FastMCP Python

Servidor MCP `openweather-fastmcp` en Python usando FastMCP, el SDK oficial de MCP para Python.

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
OPENWEATHER_API_KEY=tu_api_key just run-python-api-openweather
OPENWEATHER_API_BASE_URL=http://127.0.0.1:8100 just run-python-mcp-openweather-fastmcp
```
