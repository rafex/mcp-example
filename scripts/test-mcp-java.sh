#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

make build-java-mcp-hello >/dev/null

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
    if len(body) != content_length:
        raise RuntimeError("Unexpected EOF while reading MCP body")
    return json.loads(body.decode("utf-8"))


messages = [
    {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
    {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}},
    {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "tools/call",
        "params": {
            "name": "get_hello_languages",
            "arguments": {},
        },
    },
    {
        "jsonrpc": "2.0",
        "id": 4,
        "method": "tools/call",
        "params": {
            "name": "say_hello",
            "arguments": {
                "name": "Raúl",
                "lang": "es",
                "ip": "203.0.113.10",
            },
        },
    },
]

proc = subprocess.Popen(
    ["java", "-cp", "mcp/hello/java/build", "HelloMcpServer"],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=sys.stderr,
)

try:
    assert proc.stdin is not None
    assert proc.stdout is not None

    for message in messages:
        proc.stdin.write(frame(message))
        proc.stdin.flush()
        response = read_message(proc.stdout)
        print(json.dumps(response, ensure_ascii=False, indent=2))
        print()
finally:
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait()
PY
