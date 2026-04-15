from __future__ import annotations

import json
import os
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


OPENWEATHER_BASE_URL = os.getenv("OPENWEATHER_BASE_URL", "https://api.openweathermap.org")
OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY", "")
DEFAULT_UNITS = "metric"
DEFAULT_LANG = "en"


def call_current_weather(query: str, units: str | None = None, lang: str | None = None) -> dict[str, Any]:
    return call_openweather_json(
        "/data/2.5/weather",
        {
            "q": query,
            "units": normalize_units(units),
            "lang": lang or DEFAULT_LANG,
        },
    )


def call_weather_overview(query: str, units: str | None = None, date: str | None = None) -> dict[str, Any]:
    location = geocode_location(query)
    params = {
        "lat": str(location["lat"]),
        "lon": str(location["lon"]),
    }
    if date:
        params["date"] = date
    payload = call_openweather_json("/data/3.0/onecall/overview", params)
    payload["resolved_location"] = {
        "name": location["name"],
        "country": location["country"],
        "state": location.get("state"),
        "lat": location["lat"],
        "lon": location["lon"],
        "query": query,
        "units": normalize_units(units),
    }
    return payload


def geocode_location(query: str) -> dict[str, object]:
    payload = call_openweather_json("/geo/1.0/direct", {"q": query, "limit": "1"})
    if not isinstance(payload, list) or not payload:
        raise KeyError(query)
    first = payload[0]
    if not isinstance(first, dict):
        raise KeyError(query)
    return first


def read_service_overview() -> str:
    return "\n".join(
        [
            "OpenWeather FastMCP overview",
            "- GET https://api.openweathermap.org/data/2.5/weather?q=<city,country>&appid=<API key>",
            "- GET https://api.openweathermap.org/data/3.0/onecall/overview?lat=<lat>&lon=<lon>&appid=<API key>",
            "- GET https://api.openweathermap.org/geo/1.0/direct?q=<city,country>&limit=1&appid=<API key>",
            "- El MCP habla directo con OpenWeatherMap",
            "- No requiere backend REST intermedio",
        ]
    )


def read_unit_reference() -> str:
    return "\n".join(
        [
            "Supported units",
            "- standard: Kelvin, meter/sec",
            "- metric: Celsius, meter/sec",
            "- imperial: Fahrenheit, miles/hour",
        ]
    )


def build_current_weather_prompt(query: str, units: str | None = None) -> str:
    resolved_units = normalize_units(units)
    return (
        "Prompt para consultar el clima actual de una ubicación con OpenWeatherMap.\n\n"
        f"[system] Usa get_current_weather y resume el resultado sin inventar datos.\n"
        f"[user] Consulta el clima actual para {query} con unidades {resolved_units} usando get_current_weather."
    )


def build_weather_overview_prompt(query: str, units: str | None = None) -> str:
    resolved_units = normalize_units(units)
    return (
        "Prompt para obtener un resumen legible del tiempo con OpenWeather overview.\n\n"
        f"[system] Usa get_weather_overview y responde con el resumen devuelto.\n"
        f"[user] Obtén el weather overview para {query} con unidades {resolved_units} usando get_weather_overview."
    )


def call_openweather_json(path: str, query_params: dict[str, Any]) -> dict[str, Any]:
    if not OPENWEATHER_API_KEY:
        raise RuntimeError("Missing OPENWEATHER_API_KEY")
    params = dict(query_params)
    params["appid"] = OPENWEATHER_API_KEY
    url = f"{OPENWEATHER_BASE_URL}{path}?{urlencode(params)}"
    request = Request(url, method="GET")
    try:
        with urlopen(request, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"OpenWeatherMap responded with HTTP {exception.code}: {detail}") from exception
    except URLError as exception:
        raise RuntimeError(f"OpenWeatherMap unavailable at {OPENWEATHER_BASE_URL}: {exception.reason}") from exception


def normalize_units(units: str | None) -> str:
    if units in {"standard", "metric", "imperial"}:
        return units
    return DEFAULT_UNITS
