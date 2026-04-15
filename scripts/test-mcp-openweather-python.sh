#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

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
    body = stream.read(content_length)
    return json.loads(body.decode("utf-8"))

messages = [
    {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
    {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
    {"jsonrpc": "2.0", "id": 3, "method": "resources/list", "params": {}},
    {"jsonrpc": "2.0", "id": 4, "method": "prompts/list", "params": {}},
]

proc = subprocess.Popen(["python3", "mcp-server/openweather/python/server.py"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=sys.stderr)
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
