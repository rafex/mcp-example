from __future__ import annotations

import json
import os
import sys
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


SERVER_INFO = {"name": "openweather-python-mcp", "version": "0.1.0"}
PROTOCOL_VERSION = "2024-11-05"
OPENWEATHER_API_BASE_URL = os.getenv("OPENWEATHER_API_BASE_URL", "http://127.0.0.1:8100")


def main() -> None:
    while True:
        request = read_message()
        if request is None:
            return
        response = handle_request(request)
        if response is not None:
            write_message(response)


def read_message() -> dict[str, Any] | None:
    content_length: int | None = None
    while True:
        header_line = sys.stdin.buffer.readline()
        if not header_line:
            return None
        decoded = header_line.decode("utf-8").strip()
        if decoded == "":
            break
        if decoded.lower().startswith("content-length:"):
            content_length = int(decoded.split(":", 1)[1].strip())
    if content_length is None:
        return None
    body = sys.stdin.buffer.read(content_length)
    if not body:
        return None
    return json.loads(body.decode("utf-8"))


def write_message(payload: dict[str, Any]) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    sys.stdout.buffer.write(f"Content-Length: {len(body)}\r\n\r\n".encode("utf-8"))
    sys.stdout.buffer.write(body)
    sys.stdout.buffer.flush()


def handle_request(request: dict[str, Any]) -> dict[str, Any] | None:
    method = request.get("method")
    request_id = request.get("id")

    if method == "notifications/initialized":
        return None
    if method == "initialize":
        return success(request_id, {"protocolVersion": PROTOCOL_VERSION, "capabilities": {"tools": {}, "resources": {}, "prompts": {}}, "serverInfo": SERVER_INFO})
    if method == "tools/list":
        return success(
            request_id,
            {
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
                                "date": {"type": "string", "description": "Fecha opcional YYYY-MM-DD, hoy o mañana"},
                            },
                            "required": ["query"],
                            "additionalProperties": False,
                        },
                    },
                ]
            },
        )
    if method == "tools/call":
        params = request.get("params", {})
        name = params.get("name")
        arguments = params.get("arguments", {})
        if name == "get_current_weather":
            payload = call_api_json("/openweather/current", {"q": arguments.get("query"), "units": arguments.get("units"), "lang": arguments.get("lang")})
            return tool_result(request_id, payload)
        if name == "get_weather_overview":
            payload = call_api_json("/openweather/overview", {"q": arguments.get("query"), "units": arguments.get("units"), "date": arguments.get("date")})
            return tool_result(request_id, payload)
        return error(request_id, -32602, f"Herramienta no soportada: {name}")
    if method == "resources/list":
        return success(request_id, call_api_json("/openweather/resources"))
    if method == "resources/read":
        uri = request.get("params", {}).get("uri")
        path = resource_path(uri)
        if path is None:
            return error(request_id, -32602, f"Resource no soportado: {uri}")
        return success(request_id, call_api_json(path))
    if method == "prompts/list":
        return success(request_id, call_api_json("/openweather/prompts"))
    if method == "prompts/get":
        params = request.get("params", {})
        path = prompt_path(params.get("name"), params.get("arguments", {}))
        if path is None:
            return error(request_id, -32602, f"Prompt no soportado: {params.get('name')}")
        return success(request_id, call_api_json(path))
    return error(request_id, -32601, f"Método no encontrado: {method}")


def tool_result(request_id: Any, payload: dict[str, Any]) -> dict[str, Any]:
    return success(request_id, {"content": [{"type": "text", "text": json.dumps(payload, ensure_ascii=False)}], "structuredContent": payload, "isError": False})


def success(request_id: Any, result: dict[str, Any]) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "result": result}


def error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


def call_api_json(path: str, query_params: dict[str, Any] | None = None) -> dict[str, Any]:
    query = urlencode({key: value for key, value in (query_params or {}).items() if value})
    url = f"{OPENWEATHER_API_BASE_URL}{path}"
    if query:
        url = f"{url}?{query}"
    request = Request(url, method="GET")
    try:
        with urlopen(request, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"OpenWeather backend responded with HTTP {exception.code}: {detail}") from exception
    except URLError as exception:
        raise RuntimeError(f"OpenWeather backend unavailable at {OPENWEATHER_API_BASE_URL}: {exception.reason}") from exception


def resource_path(resource_uri: str | None) -> str | None:
    if resource_uri == "openweather://service-overview":
        return "/openweather/resources/service-overview"
    if resource_uri == "openweather://unit-reference":
        return "/openweather/resources/unit-reference"
    return None


def prompt_path(prompt_name: str | None, arguments: dict[str, Any]) -> str | None:
    if prompt_name == "current-weather-brief":
        query = urlencode({key: value for key, value in {"query": arguments.get("query"), "units": arguments.get("units")}.items() if value})
        return f"/openweather/prompts/current-weather-brief?{query}" if query else "/openweather/prompts/current-weather-brief"
    if prompt_name == "weather-overview-brief":
        query = urlencode({key: value for key, value in {"query": arguments.get("query"), "units": arguments.get("units")}.items() if value})
        return f"/openweather/prompts/weather-overview-brief?{query}" if query else "/openweather/prompts/weather-overview-brief"
    return None


if __name__ == "__main__":
    main()
