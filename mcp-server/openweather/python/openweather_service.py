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


def build_tools_payload() -> dict[str, object]:
    return {
        "tools": [
            {
                "name": "get_current_weather",
                "description": "Consulta el clima actual usando OpenWeather Current Weather API 2.5.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "query": {"type": "string", "description": "Ciudad o ciudad,country code. Ejemplo: London,uk"},
                        "units": {"type": "string", "description": "standard, metric o imperial"},
                        "lang": {"type": "string", "description": "Idioma del campo weather.description"},
                    },
                    "required": ["query"],
                    "additionalProperties": False,
                },
            },
            {
                "name": "get_weather_overview",
                "description": "Obtiene un resumen legible usando One Call API 3.0 overview.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "query": {"type": "string", "description": "Ciudad o ciudad,country code. Ejemplo: London,uk"},
                        "units": {"type": "string", "description": "standard, metric o imperial"},
                        "date": {"type": "string", "description": "Fecha opcional YYYY-MM-DD"},
                    },
                    "required": ["query"],
                    "additionalProperties": False,
                },
            },
        ]
    }


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


def build_resources_payload() -> dict[str, object]:
    return {
        "resources": [
            {
                "uri": "openweather://service-overview",
                "name": "service-overview",
                "description": "Resumen del MCP OpenWeather directo contra la API pública.",
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
                "OpenWeather MCP overview",
                "- GET https://api.openweathermap.org/data/2.5/weather?q=<city,country>&appid=<API key>",
                "- GET https://api.openweathermap.org/data/3.0/onecall/overview?lat=<lat>&lon=<lon>&appid=<API key>",
                "- GET https://api.openweathermap.org/geo/1.0/direct?q=<city,country>&limit=1&appid=<API key>",
                "- El MCP habla directo con OpenWeatherMap",
                "- No requiere backend REST intermedio",
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
                {"role": "system", "content": {"type": "text", "text": "Usa get_current_weather y resume el resultado sin inventar datos."}},
                {"role": "user", "content": {"type": "text", "text": f"Consulta el clima actual para {resolved_query} con unidades {resolved_units} usando get_current_weather."}},
            ],
        }
    if prompt_name == "weather-overview-brief":
        return {
            "description": "Prompt para obtener un resumen legible del tiempo con OpenWeather overview.",
            "messages": [
                {"role": "system", "content": {"type": "text", "text": "Usa get_weather_overview y responde con el resumen devuelto."}},
                {"role": "user", "content": {"type": "text", "text": f"Obtén el weather overview para {resolved_query} con unidades {resolved_units} usando get_weather_overview."}},
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
