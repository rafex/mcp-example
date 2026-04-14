from __future__ import annotations

import json
import os
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


HELLO_API_BASE_URL = os.getenv("HELLO_API_BASE_URL", "http://127.0.0.1:8085")


def call_hello_api(name: str | None, lang: str | None, ip: str) -> dict[str, Any]:
    return call_api_json(
        "/hello",
        query_params={
            "name": name,
            "lang": lang,
        },
        ip=ip,
    )


def call_hello_languages_api() -> dict[str, Any]:
    return call_api_json("/hello/languages")


def read_service_overview() -> str:
    payload = call_api_json("/hello/resources/service-overview")
    return extract_text_content(payload)


def read_language_reference() -> str:
    payload = call_api_json("/hello/resources/language-reference")
    return extract_text_content(payload)


def build_greet_user_prompt(name: str, lang: str) -> str:
    payload = call_api_json(
        "/hello/prompts/greet-user",
        query_params={
            "name": name,
            "lang": lang,
        },
    )
    return prompt_payload_to_text(payload)


def build_language_report_prompt(name: str) -> str:
    payload = call_api_json(
        "/hello/prompts/language-report",
        query_params={"name": name},
    )
    return prompt_payload_to_text(payload)


def call_api_json(
    path: str,
    query_params: dict[str, Any] | None = None,
    ip: str | None = None,
) -> dict[str, Any]:
    query = urlencode({key: value for key, value in (query_params or {}).items() if value})
    url = f"{HELLO_API_BASE_URL}{path}"
    if query:
        url = f"{url}?{query}"

    headers: dict[str, str] = {}
    if ip:
        headers["X-Forwarded-For"] = ip

    request = Request(url, method="GET", headers=headers)
    try:
        with urlopen(request, timeout=5) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"REST backend responded with HTTP {exception.code}: {detail}") from exception
    except URLError as exception:
        raise RuntimeError(f"REST backend unavailable at {HELLO_API_BASE_URL}: {exception.reason}") from exception


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
