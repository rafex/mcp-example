from __future__ import annotations

import argparse
import asyncio
import json
import os
import socket
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from mcp import ClientSession
from mcp.client.stdio import StdioServerParameters, stdio_client


PROJECT_ROOT = Path(__file__).resolve().parent
REPO_ROOT = PROJECT_ROOT.parent
VENV_PYTHON = PROJECT_ROOT / ".venv" / "bin" / "python"
SYSTEM_PYTHON = os.environ.get("MCP_CLIENT_SYSTEM_PYTHON", "/usr/bin/python3")
HOST = "127.0.0.1"


@dataclass(frozen=True)
class TargetConfig:
    example: str
    language: str
    transport: str
    backend_port: int
    backend_env_key: str
    backend_command: list[str]
    mcp_command: list[str]
    build_commands: list[list[str]]
    extra_env: dict[str, str]
    resource_uri: str
    prompt_name: str
    prompt_arguments: dict[str, str]
    tool_calls: list[dict[str, Any]]


TARGETS: dict[tuple[str, str], TargetConfig] = {
    ("hello", "python"): TargetConfig(
        example="hello",
        language="python",
        transport="manual",
        backend_port=18080,
        backend_env_key="HELLO_API_BASE_URL",
        backend_command=[SYSTEM_PYTHON, "backend/api-hello/python/server.py"],
        mcp_command=[SYSTEM_PYTHON, "mcp-server/hello/python/server.py"],
        build_commands=[],
        extra_env={},
        resource_uri="hello://service-overview",
        prompt_name="greet-user",
        prompt_arguments={"name": "Raúl", "lang": "es"},
        tool_calls=[
            {"name": "get_hello_languages", "arguments": {}},
            {"name": "say_hello", "arguments": {"name": "Raúl", "lang": "es", "ip": "203.0.113.10"}},
        ],
    ),
    ("hello", "java"): TargetConfig(
        example="hello",
        language="java",
        transport="manual",
        backend_port=18081,
        backend_env_key="HELLO_API_BASE_URL",
        backend_command=["java", "-cp", "backend/api-hello/java/build", "HelloApiServer"],
        mcp_command=["java", "-cp", "mcp-server/hello/java/build", "HelloMcpServer"],
        build_commands=[["make", "build-java-api-hello"], ["make", "build-java-mcp-hello"]],
        extra_env={},
        resource_uri="hello://service-overview",
        prompt_name="greet-user",
        prompt_arguments={"name": "Raúl", "lang": "es"},
        tool_calls=[
            {"name": "get_hello_languages", "arguments": {}},
            {"name": "say_hello", "arguments": {"name": "Raúl", "lang": "es", "ip": "203.0.113.10"}},
        ],
    ),
    ("hello-fastmcp", "python"): TargetConfig(
        example="hello-fastmcp",
        language="python",
        transport="sdk",
        backend_port=18082,
        backend_env_key="HELLO_API_BASE_URL",
        backend_command=[SYSTEM_PYTHON, "backend/api-hello/python/server.py"],
        mcp_command=[str(VENV_PYTHON), "mcp-server/hello-fastmcp/python/server.py"],
        build_commands=[],
        extra_env={},
        resource_uri="hello://service-overview",
        prompt_name="greet_user",
        prompt_arguments={"name": "Raúl", "lang": "es"},
        tool_calls=[
            {"name": "get_hello_languages", "arguments": {}},
            {"name": "say_hello", "arguments": {"name": "Raúl", "lang": "es", "ip": "203.0.113.10"}},
        ],
    ),
    ("date", "python"): TargetConfig(
        example="date",
        language="python",
        transport="manual",
        backend_port=19090,
        backend_env_key="DATE_API_BASE_URL",
        backend_command=[SYSTEM_PYTHON, "backend/api-date/python/server.py"],
        mcp_command=[SYSTEM_PYTHON, "mcp-server/date/python/server.py"],
        build_commands=[],
        extra_env={"DATE_API_TOKEN": "dev-date-token", "DATE_API_CLIENT_ID": "mcp-date-client"},
        resource_uri="date://auth-reference",
        prompt_name="single-location-time",
        prompt_arguments={"location": "mx-central"},
        tool_calls=[
            {"name": "list_supported_locations", "arguments": {}},
            {"name": "get_current_time", "arguments": {"location": "jp", "ip": "203.0.113.20"}},
        ],
    ),
    ("date", "java"): TargetConfig(
        example="date",
        language="java",
        transport="manual",
        backend_port=19091,
        backend_env_key="DATE_API_BASE_URL",
        backend_command=["java", "-cp", "backend/api-date/java/build", "DateApiServer"],
        mcp_command=["java", "-cp", "mcp-server/date/java/build", "DateMcpServer"],
        build_commands=[["make", "build-java-api-date"], ["make", "build-java-mcp-date"]],
        extra_env={"DATE_API_TOKEN": "dev-date-token", "DATE_API_CLIENT_ID": "mcp-date-client"},
        resource_uri="date://auth-reference",
        prompt_name="single-location-time",
        prompt_arguments={"location": "mx-central"},
        tool_calls=[
            {"name": "list_supported_locations", "arguments": {}},
            {"name": "get_current_time", "arguments": {"location": "jp", "ip": "203.0.113.20"}},
        ],
    ),
    ("openweather", "python"): TargetConfig(
        example="openweather",
        language="python",
        transport="sdk",
        backend_port=18100,
        backend_env_key="OPENWEATHER_API_BASE_URL",
        backend_command=[SYSTEM_PYTHON, "backend/api-openweather/python/server.py"],
        mcp_command=[str(VENV_PYTHON), "mcp-server/openweather/python/server.py"],
        build_commands=[],
        extra_env={},
        resource_uri="openweather://service-overview",
        prompt_name="current_weather_brief",
        prompt_arguments={"query": "London,uk", "units": "metric"},
        tool_calls=[
            {"name": "get_current_weather", "arguments": {"query": "London,uk", "units": "metric", "lang": "en"}},
            {"name": "get_weather_overview", "arguments": {"query": "London,uk", "units": "metric"}},
        ],
    ),
}


class McpClientError(RuntimeError):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Cliente MCP para probar todos los ejemplos del repositorio."
    )
    parser.add_argument("example", choices=["hello", "hello-fastmcp", "date", "openweather"])
    parser.add_argument("language", choices=["python", "java"])
    parser.add_argument(
        "--catalog-only",
        action="store_true",
        help="Solo lista tools, resources y prompts; omite lecturas y llamadas.",
    )
    parser.add_argument(
        "--startup-wait",
        type=float,
        default=1.5,
        help="Segundos de espera para que el backend levante antes de conectar el cliente.",
    )
    parser.add_argument(
        "--base-url",
        help="URL base del backend REST a reutilizar. Si se indica, el cliente no levanta backend local.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    target_key = (args.example, args.language)
    config = TARGETS.get(target_key)
    if config is None:
        raise McpClientError(f"No existe un target MCP para example={args.example} language={args.language}")
    external_base_url = resolve_external_base_url(config, args.base_url)

    for build_command in config.build_commands:
        run_command(build_command)

    env = build_env(config, external_base_url)
    backend = None
    if external_base_url is None:
        backend = start_process(
            config.backend_command,
            env,
            stderr_path=f"/tmp/mcp-example-{config.example}-{config.language}-backend.log",
        )
        time.sleep(args.startup_wait)

    print_header(config, env[config.backend_env_key], external_base_url is None)

    try:
        if config.transport == "manual":
            run_manual_transport(config, env, args.catalog_only)
        elif config.transport == "sdk":
            asyncio.run(run_sdk_transport(config, env, args.catalog_only))
        else:
            raise McpClientError(f"Transporte no soportado: {config.transport}")
        return 0
    finally:
        stop_process(backend)


def resolve_external_base_url(config: TargetConfig, cli_base_url: str | None) -> str | None:
    if cli_base_url:
        return cli_base_url
    return os.environ.get(config.backend_env_key)


def build_env(config: TargetConfig, external_base_url: str | None) -> dict[str, str]:
    env = os.environ.copy()
    env["PYTHONUNBUFFERED"] = "1"
    if external_base_url is None:
        backend_port = reserve_local_port()
        if config.backend_env_key == "HELLO_API_BASE_URL":
            env["HELLO_API_PORT"] = str(backend_port)
        if config.backend_env_key == "DATE_API_BASE_URL":
            env["DATE_API_PORT"] = str(backend_port)
        if config.backend_env_key == "OPENWEATHER_API_BASE_URL":
            env["OPENWEATHER_API_PORT"] = str(backend_port)
        env[config.backend_env_key] = f"http://{HOST}:{backend_port}"
    else:
        env[config.backend_env_key] = external_base_url
    env.update(config.extra_env)
    return env


def reserve_local_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind((HOST, 0))
        sock.listen(1)
        return int(sock.getsockname()[1])


def run_command(command: list[str]) -> None:
    completed = subprocess.run(
        command,
        cwd=REPO_ROOT,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        message = f"Fallo al ejecutar: {' '.join(command)}"
        if completed.stderr.strip():
            message = f"{message}\n{completed.stderr.strip()}"
        elif completed.stdout.strip():
            message = f"{message}\n{completed.stdout.strip()}"
        raise McpClientError(message)


def start_process(command: list[str], env: dict[str, str], stderr_path: str) -> subprocess.Popen[bytes]:
    stderr_handle = open(stderr_path, "wb")
    try:
        return subprocess.Popen(
            command,
            cwd=REPO_ROOT,
            env=env,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=stderr_handle,
        )
    except Exception:
        stderr_handle.close()
        raise


def stop_process(process: subprocess.Popen[bytes] | None) -> None:
    if process is None:
        return
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
    stderr_stream = process.stderr
    if stderr_stream is not None:
        stderr_stream.close()


def run_manual_transport(config: TargetConfig, env: dict[str, str], catalog_only: bool) -> None:
    mcp = start_process(
        config.mcp_command,
        env,
        stderr_path=f"/tmp/mcp-example-{config.example}-{config.language}-mcp.log",
    )
    try:
        assert mcp.stdin is not None
        assert mcp.stdout is not None

        exchange_manual(mcp, 1, "initialize", {})
        exchange_manual(mcp, 2, "notifications/initialized", expect_response=False)

        tools = exchange_manual(mcp, 3, "tools/list", {})
        print_section("tools/list", tools)

        resources = exchange_manual(mcp, 4, "resources/list", {})
        print_section("resources/list", resources)

        prompts = exchange_manual(mcp, 5, "prompts/list", {})
        print_section("prompts/list", prompts)

        if catalog_only:
            return

        resource = exchange_manual(mcp, 6, "resources/read", {"uri": config.resource_uri})
        print_section("resources/read", resource)

        prompt = exchange_manual(
            mcp,
            7,
            "prompts/get",
            {"name": config.prompt_name, "arguments": config.prompt_arguments},
        )
        print_section("prompts/get", prompt)

        next_id = 8
        for tool_call in config.tool_calls:
            result = exchange_manual(
                mcp,
                next_id,
                "tools/call",
                {"name": tool_call["name"], "arguments": tool_call["arguments"]},
            )
            print_section(f"tools/call:{tool_call['name']}", result)
            next_id += 1
    finally:
        stop_process(mcp)


async def run_sdk_transport(config: TargetConfig, env: dict[str, str], catalog_only: bool) -> None:
    params = StdioServerParameters(
        command=config.mcp_command[0],
        args=config.mcp_command[1:],
        env=env,
        cwd=str(REPO_ROOT),
    )

    with open(os.devnull, "w", encoding="utf-8") as errlog:
        async with stdio_client(params, errlog=errlog) as (read_stream, write_stream):
            async with ClientSession(read_stream, write_stream) as session:
                initialize_result = await session.initialize()
                print_section("initialize", initialize_result.model_dump(mode="json", by_alias=True, exclude_none=True))

                tools = await session.list_tools()
                print_section("tools/list", tools.model_dump(mode="json", by_alias=True, exclude_none=True))

                resources = await session.list_resources()
                print_section("resources/list", resources.model_dump(mode="json", by_alias=True, exclude_none=True))

                prompts = await session.list_prompts()
                print_section("prompts/list", prompts.model_dump(mode="json", by_alias=True, exclude_none=True))

                if catalog_only:
                    return

                resource = await session.read_resource(config.resource_uri)
                print_section("resources/read", resource.model_dump(mode="json", by_alias=True, exclude_none=True))

                prompt = await session.get_prompt(config.prompt_name, config.prompt_arguments)
                print_section("prompts/get", prompt.model_dump(mode="json", by_alias=True, exclude_none=True))

                for tool_call in config.tool_calls:
                    result = await session.call_tool(tool_call["name"], tool_call["arguments"])
                    print_section(
                        f"tools/call:{tool_call['name']}",
                        result.model_dump(mode="json", by_alias=True, exclude_none=True),
                    )


def exchange_manual(
    process: subprocess.Popen[bytes],
    request_id: int,
    method: str,
    params: dict[str, Any] | None = None,
    expect_response: bool = True,
) -> dict[str, Any]:
    assert process.stdin is not None
    payload: dict[str, Any] = {"jsonrpc": "2.0", "method": method}
    if expect_response:
        payload["id"] = request_id
    if params is not None:
        payload["params"] = params
    write_message(process.stdin, payload)
    if not expect_response:
        return {}
    assert process.stdout is not None
    response = read_message(process.stdout)
    if "error" in response:
        raise McpClientError(json.dumps(response, ensure_ascii=False, indent=2))
    return response


def write_message(stdin: Any, payload: dict[str, Any]) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    stdin.write(f"Content-Length: {len(body)}\r\n\r\n".encode("utf-8"))
    stdin.write(body)
    stdin.flush()


def read_message(stdout: Any) -> dict[str, Any]:
    header = b""
    while b"\r\n\r\n" not in header:
        chunk = stdout.read(1)
        if not chunk:
            raise McpClientError("EOF inesperado leyendo cabeceras MCP")
        header += chunk

    content_length = None
    for line in header.decode("utf-8").split("\r\n"):
        if line.lower().startswith("content-length:"):
            content_length = int(line.split(":", 1)[1].strip())
            break
    if content_length is None:
        raise McpClientError("Falta Content-Length en la respuesta MCP")

    body = stdout.read(content_length)
    if len(body) != content_length:
        raise McpClientError("EOF inesperado leyendo el cuerpo MCP")
    return json.loads(body.decode("utf-8"))


def print_header(config: TargetConfig, backend_url: str, spawned_backend: bool) -> None:
    print(f"mcp-client: example={config.example} language={config.language} transport={config.transport}")
    print(f"backend: {backend_url}")
    print(f"backend_mode: {'spawned' if spawned_backend else 'external'}")
    print()


def print_section(title: str, payload: dict[str, Any]) -> None:
    print(f"=== {title} ===")
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    print()


if __name__ == "__main__":
    raise SystemExit(main())
