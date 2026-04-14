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


def fetch_current_weather(query: str, units: str | None = None, lang: str | None = None) -> dict[str, Any]:
    return call_openweather_json(
        "/data/2.5/weather",
        {
            "q": query,
            "units": normalize_units(units),
            "lang": lang or DEFAULT_LANG,
        },
    )


def fetch_weather_overview(query: str, units: str | None = None, date: str | None = None) -> dict[str, Any]:
    location = geocode_location(query)
    params: dict[str, str] = {
        "lat": str(location["lat"]),
        "lon": str(location["lon"]),
        "units": normalize_units(units),
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
    }
    return payload


def geocode_location(query: str) -> dict[str, object]:
    payload = call_openweather_json(
        "/geo/1.0/direct",
        {
            "q": query,
            "limit": "1",
        },
    )
    if not isinstance(payload, list) or not payload:
        raise KeyError(query)
    first = payload[0]
    if not isinstance(first, dict):
        raise KeyError(query)
    return first


def build_resources_payload() -> dict[str, object]:
    return {
        "resources": [
            {
                "uri": "openweather://service-overview",
                "name": "service-overview",
                "description": "Resumen de los endpoints del wrapper OpenWeatherMap.",
                "mimeType": "text/plain",
            },
            {
                "uri": "openweather://unit-reference",
                "name": "unit-reference",
                "description": "Referencia de unidades soportadas por OpenWeatherMap.",
                "mimeType": "text/plain",
            },
        ]
    }


def build_resource_contents_payload(resource_uri: str) -> dict[str, object]:
    if resource_uri == "openweather://service-overview":
        text = "\n".join(
            [
                "OpenWeather wrapper overview",
                "- GET /openweather/current?q=<city,country>",
                "- GET /openweather/overview?q=<city,country>",
                "- GET /openweather/resources",
                "- GET /openweather/prompts",
                "- Uses Current Weather API 2.5",
                "- Uses One Call API 3.0 overview",
            ]
        )
        return {"contents": [{"uri": resource_uri, "mimeType": "text/plain", "text": text}]}

    if resource_uri == "openweather://unit-reference":
        text = "\n".join(
            [
                "Supported units",
                "- standard: Kelvin, meter/sec",
                "- metric: Celsius, meter/sec",
                "- imperial: Fahrenheit, miles/hour",
            ]
        )
        return {"contents": [{"uri": resource_uri, "mimeType": "text/plain", "text": text}]}

    raise KeyError(resource_uri)


def build_prompts_payload() -> dict[str, object]:
    return {
        "prompts": [
            {
                "name": "current-weather-brief",
                "description": "Pide consultar el clima actual de una ciudad con get_current_weather.",
                "arguments": [
                    {"name": "query", "description": "Ciudad o ciudad,country code. Ejemplo: London,uk", "required": True},
                    {"name": "units", "description": "standard, metric o imperial", "required": False},
                ],
            },
            {
                "name": "weather-overview-brief",
                "description": "Pide un resumen legible del tiempo usando get_weather_overview.",
                "arguments": [
                    {"name": "query", "description": "Ciudad o ciudad,country code. Ejemplo: London,uk", "required": True},
                    {"name": "units", "description": "standard, metric o imperial", "required": False},
                ],
            },
        ]
    }


def build_prompt_details_payload(prompt_name: str, query: str | None = None, units: str | None = None) -> dict[str, object]:
    resolved_query = query or "London,uk"
    resolved_units = normalize_units(units)

    if prompt_name == "current-weather-brief":
        return {
            "description": "Prompt para consultar el clima actual de una ubicación con OpenWeatherMap.",
            "messages": [
                {
                    "role": "system",
                    "content": {"type": "text", "text": "Usa la herramienta get_current_weather y resume el resultado sin inventar datos."},
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": f"Consulta el clima actual para {resolved_query} con unidades {resolved_units} usando get_current_weather.",
                    },
                },
            ],
        }

    if prompt_name == "weather-overview-brief":
        return {
            "description": "Prompt para obtener un resumen legible del tiempo con OpenWeather overview.",
            "messages": [
                {
                    "role": "system",
                    "content": {"type": "text", "text": "Usa la herramienta get_weather_overview y responde con el resumen devuelto."},
                },
                {
                    "role": "user",
                    "content": {
                        "type": "text",
                        "text": f"Obtén el weather overview para {resolved_query} con unidades {resolved_units} usando get_weather_overview.",
                    },
                },
            ],
        }

    raise KeyError(prompt_name)


def normalize_units(units: str | None) -> str:
    if units in {"standard", "metric", "imperial"}:
        return units
    return DEFAULT_UNITS


def call_openweather_json(path: str, query_params: dict[str, str]) -> Any:
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
