from __future__ import annotations

import json
import os
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


OPENWEATHER_API_BASE_URL = os.getenv("OPENWEATHER_API_BASE_URL", "http://127.0.0.1:8100")


def call_current_weather(query: str, units: str | None = None, lang: str | None = None) -> dict[str, Any]:
    return call_api_json(
        "/openweather/current",
        {
            "q": query,
            "units": units,
            "lang": lang,
        },
    )


def call_weather_overview(query: str, units: str | None = None, date: str | None = None) -> dict[str, Any]:
    return call_api_json(
        "/openweather/overview",
        {
            "q": query,
            "units": units,
            "date": date,
        },
    )


def read_service_overview() -> str:
    return extract_text_content(call_api_json("/openweather/resources/service-overview"))


def read_unit_reference() -> str:
    return extract_text_content(call_api_json("/openweather/resources/unit-reference"))


def build_current_weather_prompt(query: str, units: str | None = None) -> str:
    return prompt_payload_to_text(
        call_api_json(
            "/openweather/prompts/current-weather-brief",
            {
                "query": query,
                "units": units,
            },
        )
    )


def build_weather_overview_prompt(query: str, units: str | None = None) -> str:
    return prompt_payload_to_text(
        call_api_json(
            "/openweather/prompts/weather-overview-brief",
            {
                "query": query,
                "units": units,
            },
        )
    )


def call_api_json(path: str, query_params: dict[str, Any] | None = None) -> dict[str, Any]:
    query = urlencode({key: value for key, value in (query_params or {}).items() if value is not None and value != ""})
    url = f"{OPENWEATHER_API_BASE_URL}{path}"
    if query:
        url = f"{url}?{query}"

    request = Request(url, method="GET")
    try:
        with urlopen(request, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"REST backend responded with HTTP {exception.code}: {detail}") from exception
    except URLError as exception:
        raise RuntimeError(f"REST backend unavailable at {OPENWEATHER_API_BASE_URL}: {exception.reason}") from exception


def extract_text_content(payload: dict[str, Any]) -> str:
    contents = payload.get("contents", [])
    if not contents:
        return ""
    first = contents[0]
    if isinstance(first, dict):
        return str(first.get("text", ""))
    return ""


def prompt_payload_to_text(payload: dict[str, Any]) -> str:
    lines: list[str] = []
    description = payload.get("description")
    if description:
        lines.append(str(description))
        lines.append("")

    for message in payload.get("messages", []):
        if not isinstance(message, dict):
            continue
        role = message.get("role", "user")
        content = message.get("content", {})
        if isinstance(content, dict):
            text = str(content.get("text", "")).strip()
            if text:
                lines.append(f"[{role}] {text}")

    return "\n".join(lines).strip()
