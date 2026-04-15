from __future__ import annotations

import json
import sys
from typing import Any

from openweather_service import (
    build_prompt_details_payload,
    build_prompts_payload,
    build_resource_contents_payload,
    build_resources_payload,
    build_tools_payload,
    fetch_current_weather,
    fetch_weather_overview,
)


SERVER_INFO = {"name": "openweather-python-mcp", "version": "0.1.0"}
PROTOCOL_VERSION = "2024-11-05"


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
        return success(request_id, build_tools_payload())
    if method == "tools/call":
        params = request.get("params", {})
        name = params.get("name")
        arguments = params.get("arguments", {})
        if name == "get_current_weather":
            payload = fetch_current_weather(arguments.get("query"), arguments.get("units"), arguments.get("lang"))
            return tool_result(request_id, payload)
        if name == "get_weather_overview":
            payload = fetch_weather_overview(arguments.get("query"), arguments.get("units"), arguments.get("date"))
            return tool_result(request_id, payload)
        return error(request_id, -32602, f"Herramienta no soportada: {name}")
    if method == "resources/list":
        return success(request_id, build_resources_payload())
    if method == "resources/read":
        uri = request.get("params", {}).get("uri")
        try:
            payload = build_resource_contents_payload(uri)
        except KeyError:
            return error(request_id, -32602, f"Resource no soportado: {uri}")
        return success(request_id, payload)
    if method == "prompts/list":
        return success(request_id, build_prompts_payload())
    if method == "prompts/get":
        params = request.get("params", {})
        arguments = params.get("arguments", {})
        try:
            payload = build_prompt_details_payload(params.get("name"), arguments.get("query"), arguments.get("units"))
        except KeyError:
            return error(request_id, -32602, f"Prompt no soportado: {params.get('name')}")
        return success(request_id, payload)
    return error(request_id, -32601, f"Método no encontrado: {method}")


def tool_result(request_id: Any, payload: dict[str, Any]) -> dict[str, Any]:
    return success(request_id, {"content": [{"type": "text", "text": json.dumps(payload, ensure_ascii=False)}], "structuredContent": payload, "isError": False})


def success(request_id: Any, result: dict[str, Any]) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "result": result}


def error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


if __name__ == "__main__":
    main()
