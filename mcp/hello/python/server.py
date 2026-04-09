from __future__ import annotations

import json
import sys
from typing import Any

from hello_service import build_hello_payload


SERVER_INFO = {"name": "hello-python-mcp", "version": "0.1.0"}
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
    header = f"Content-Length: {len(body)}\r\n\r\n".encode("utf-8")
    sys.stdout.buffer.write(header)
    sys.stdout.buffer.write(body)
    sys.stdout.buffer.flush()


def handle_request(request: dict[str, Any]) -> dict[str, Any] | None:
    method = request.get("method")
    request_id = request.get("id")

    if method == "notifications/initialized":
        return None

    if method == "initialize":
        return success(
            request_id,
            {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {"tools": {}},
                "serverInfo": SERVER_INFO,
            },
        )

    if method == "tools/list":
        return success(
            request_id,
            {
                "tools": [
                    {
                        "name": "say_hello",
                        "description": "Devuelve un saludo opcionalmente personalizado y localizado.",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string", "description": "Nombre opcional a saludar."},
                                "lang": {"type": "string", "description": "Idioma del saludo, por ejemplo en o es."},
                                "ip": {"type": "string", "description": "IP opcional a reflejar en la respuesta."},
                            },
                            "additionalProperties": False,
                        },
                    }
                ]
            },
        )

    if method == "tools/call":
        params = request.get("params", {})
        tool_name = params.get("name")
        arguments = params.get("arguments", {})
        if tool_name != "say_hello":
            return error(request_id, -32602, f"Herramienta no soportada: {tool_name}")

        payload = build_hello_payload(
            name=arguments.get("name"),
            lang=arguments.get("lang"),
            ip=arguments.get("ip") or "127.0.0.1",
        )
        return success(
            request_id,
            {
                "content": [{"type": "text", "text": json.dumps(payload, ensure_ascii=False)}],
                "structuredContent": payload,
                "isError": False,
            },
        )

    return error(request_id, -32601, f"Método no encontrado: {method}")


def success(request_id: Any, result: dict[str, Any]) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "result": result}


def error(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


if __name__ == "__main__":
    main()
