from __future__ import annotations

import sys
from typing import Any

try:
    from mcp.server.fastmcp import FastMCP
except ModuleNotFoundError as exception:
    print(
        'Missing dependency: install the official MCP Python SDK with `pip install "mcp[cli]"`.',
        file=sys.stderr,
    )
    raise

from hello_api_client import (
    build_greet_user_prompt,
    build_language_report_prompt,
    call_hello_api,
    call_hello_languages_api,
    read_language_reference,
    read_service_overview,
)


mcp = FastMCP("hello-fastmcp")


@mcp.tool()
def say_hello(name: str | None = None, lang: str | None = None, ip: str = "127.0.0.1") -> dict[str, Any]:
    """Devuelve un saludo opcionalmente personalizado y localizado usando el backend REST hello."""
    return call_hello_api(name=name, lang=lang, ip=ip)


@mcp.tool()
def get_hello_languages() -> dict[str, Any]:
    """Devuelve la cantidad de idiomas soportados y sus códigos usando el backend REST hello."""
    return call_hello_languages_api()


@mcp.resource("hello://service-overview")
def service_overview() -> str:
    """Resumen de los endpoints REST disponibles del ejemplo hello."""
    return read_service_overview()


@mcp.resource("hello://language-reference")
def language_reference() -> str:
    """Referencia de idiomas soportados y su saludo base."""
    return read_language_reference()


@mcp.prompt()
def greet_user(name: str, lang: str) -> str:
    """Genera instrucciones para saludar a una persona en un idioma concreto usando say_hello."""
    return build_greet_user_prompt(name=name, lang=lang)


@mcp.prompt()
def language_report(name: str) -> str:
    """Genera instrucciones para listar idiomas y luego saludar a alguien en todos ellos."""
    return build_language_report_prompt(name=name)


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
