#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

export DATE_API_PORT=19090
export DATE_API_BASE_URL="http://127.0.0.1:${DATE_API_PORT}"
export DATE_API_TOKEN=dev-date-token
export DATE_API_CLIENT_ID=mcp-date-client

python3 backend/api-date/python/server.py >/tmp/mcp-example-date-python-api.log 2>&1 &
BACKEND_PID=$!
trap 'kill "$BACKEND_PID" 2>/dev/null || true' EXIT
sleep 1

python3 - <<'PY'
import json
import subprocess
import sys

def frame(payload: dict) -> bytes:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    return f"Content-Length: {len(body)}\r\n\r\n".encode("utf-8") + body

def read_message(stream) -> dict:
    header = b""
    while b"\r\n\r\n" not in header:
        chunk = stream.read(1)
        if not chunk:
            raise RuntimeError("Unexpected EOF while reading MCP header")
        header += chunk
    content_length = None
    for line in header.decode("utf-8").split("\r\n"):
        if line.lower().startswith("content-length:"):
            content_length = int(line.split(":", 1)[1].strip())
            break
    if content_length is None:
        raise RuntimeError("Missing Content-Length header")
    body = stream.read(content_length)
    return json.loads(body.decode("utf-8"))

messages = [
    {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
    {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
    {"jsonrpc": "2.0", "id": 3, "method": "resources/list", "params": {}},
    {"jsonrpc": "2.0", "id": 4, "method": "resources/read", "params": {"uri": "date://auth-reference"}},
    {"jsonrpc": "2.0", "id": 5, "method": "prompts/list", "params": {}},
    {"jsonrpc": "2.0", "id": 6, "method": "prompts/get", "params": {"name": "single-location-time", "arguments": {"location": "mx-central"}}},
    {"jsonrpc": "2.0", "id": 7, "method": "tools/call", "params": {"name": "list_supported_locations", "arguments": {}}},
    {"jsonrpc": "2.0", "id": 8, "method": "tools/call", "params": {"name": "get_current_time", "arguments": {"location": "jp", "ip": "203.0.113.20"}}},
]

proc = subprocess.Popen(["python3", "mcp/date/python/server.py"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=sys.stderr)
try:
    assert proc.stdin and proc.stdout
    for message in messages:
        proc.stdin.write(frame(message))
        proc.stdin.flush()
        print(json.dumps(read_message(proc.stdout), ensure_ascii=False, indent=2))
        print()
finally:
    proc.terminate()
    proc.wait(timeout=5)
PY
