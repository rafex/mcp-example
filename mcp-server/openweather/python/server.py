from __future__ import annotations

import sys
from typing import Any

try:
    from mcp.server.fastmcp import FastMCP
except ModuleNotFoundError:
    print(
        'Missing dependency: install the official MCP Python SDK with `pip install "mcp[cli]"`.',
        file=sys.stderr,
    )
    raise

from openweather_api_client import (
    build_current_weather_prompt,
    build_weather_overview_prompt,
    call_current_weather,
    call_weather_overview,
    read_service_overview,
    read_unit_reference,
)


mcp = FastMCP("openweather-fastmcp")


@mcp.tool()
def get_current_weather(query: str, units: str | None = None, lang: str | None = None) -> dict[str, Any]:
    """Consulta el clima actual usando OpenWeather Current Weather API 2.5."""
    return call_current_weather(query=query, units=units, lang=lang)


@mcp.tool()
def get_weather_overview(query: str, units: str | None = None, date: str | None = None) -> dict[str, Any]:
    """Obtiene un resumen legible usando One Call API 3.0 overview."""
    return call_weather_overview(query=query, units=units, date=date)


@mcp.resource("openweather://service-overview")
def service_overview() -> str:
    """Resumen de los endpoints del wrapper OpenWeatherMap."""
    return read_service_overview()


@mcp.resource("openweather://unit-reference")
def unit_reference() -> str:
    """Referencia de unidades soportadas por OpenWeatherMap."""
    return read_unit_reference()


@mcp.prompt()
def current_weather_brief(query: str, units: str = "metric") -> str:
    """Genera instrucciones para consultar el clima actual con get_current_weather."""
    return build_current_weather_prompt(query=query, units=units)


@mcp.prompt()
def weather_overview_brief(query: str, units: str = "metric") -> str:
    """Genera instrucciones para consultar el weather overview con get_weather_overview."""
    return build_weather_overview_prompt(query=query, units=units)


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
