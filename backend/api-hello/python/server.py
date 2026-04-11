from __future__ import annotations

import json
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from hello_service import (
    build_hello_payload,
    build_languages_payload,
    build_prompt_details_payload,
    build_prompts_payload,
    build_resource_contents_payload,
    build_resources_payload,
)


HOST = os.getenv("HELLO_API_HOST", "127.0.0.1")
PORT = int(os.getenv("HELLO_API_PORT", "8080"))
ALLOWED_METHODS = "GET, POST, OPTIONS"
LOGS_DIR = Path(__file__).resolve().parents[3] / "logs"
LOG_FILE = LOGS_DIR / "backend-api-hello-python.log"


def configure_logger() -> logging.Logger:
    LOGS_DIR.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger("backend.api_hello.python")
    if logger.handlers:
        return logger

    logger.setLevel(logging.INFO)
    logger.propagate = False

    file_handler = logging.FileHandler(LOG_FILE, encoding="utf-8")
    file_handler.setFormatter(
        logging.Formatter("%(asctime)s %(levelname)s %(name)s %(message)s")
    )
    logger.addHandler(file_handler)
    return logger


LOGGER = configure_logger()


class HelloHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        if parsed_url.path == "/hello":
            query_params = parse_qs(parsed_url.query)
            self.handle_hello(
                name=first_value(query_params, "name"),
                lang=first_value(query_params, "lang"),
            )
            return

        if parsed_url.path == "/hello/languages":
            self.handle_languages()
            return

        if parsed_url.path == "/hello/resources":
            self.handle_resources()
            return

        if parsed_url.path.startswith("/hello/resources/"):
            self.handle_resource(parsed_url.path)
            return

        if parsed_url.path == "/hello/prompts":
            self.handle_prompts()
            return

        if parsed_url.path.startswith("/hello/prompts/"):
            self.handle_prompt(parsed_url)
            return

        self.handle_not_found(parsed_url.path)

    def do_POST(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        allow_methods = allowed_methods_for_path(parsed_url.path)
        if allow_methods is None:
            self.handle_not_found(parsed_url.path)
            return

        if parsed_url.path != "/hello":
            self.send_json({"error": "Method Not Allowed"}, HTTPStatus.METHOD_NOT_ALLOWED, allow_methods)
            return

        payload = self.read_json_body()
        if payload is None:
            self.send_json({"error": "Invalid JSON body"}, HTTPStatus.BAD_REQUEST)
            return

        name = payload.get("name")
        lang = payload.get("lang")
        if name is not None and not isinstance(name, str):
            self.send_json({"error": "Field 'name' must be a string"}, HTTPStatus.BAD_REQUEST)
            return
        if lang is not None and not isinstance(lang, str):
            self.send_json({"error": "Field 'lang' must be a string"}, HTTPStatus.BAD_REQUEST)
            return

        self.handle_hello(name=name, lang=lang)

    def do_OPTIONS(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        allow_methods = allowed_methods_for_path(parsed_url.path)
        if allow_methods is None:
            self.handle_not_found(parsed_url.path)
            return

        client_ip = resolve_client_ip(self)
        LOGGER.info(
            "request path=%s method=OPTIONS status=%s client_ip=%s",
            parsed_url.path,
            HTTPStatus.NO_CONTENT.value,
            client_ip,
        )
        self.send_response(HTTPStatus.NO_CONTENT.value)
        self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return

    def send_json(self, payload: dict[str, object], status: HTTPStatus, allow_methods: str | None = None) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        if allow_methods is not None:
            self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def handle_hello(self, name: str | None, lang: str | None) -> None:
        client_ip = resolve_client_ip(self)
        payload = build_hello_payload(name=name, lang=lang, ip=client_ip)
        LOGGER.info(
            "request path=/hello method=%s status=%s client_ip=%s lang=%s has_name=%s",
            self.command,
            HTTPStatus.OK.value,
            client_ip,
            payload["lang"],
            payload["has_name"],
        )
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_languages(self) -> None:
        client_ip = resolve_client_ip(self)
        payload = build_languages_payload()
        LOGGER.info(
            "request path=/hello/languages method=%s status=%s client_ip=%s language_count=%s",
            self.command,
            HTTPStatus.OK.value,
            client_ip,
            payload["language_count"],
        )
        self.send_json(payload, HTTPStatus.OK, "GET, OPTIONS")

    def handle_resources(self) -> None:
        client_ip = resolve_client_ip(self)
        payload = build_resources_payload()
        LOGGER.info(
            "request path=/hello/resources method=%s status=%s client_ip=%s resource_count=%s",
            self.command,
            HTTPStatus.OK.value,
            client_ip,
            len(payload["resources"]),
        )
        self.send_json(payload, HTTPStatus.OK, "GET, OPTIONS")

    def handle_resource(self, path: str) -> None:
        client_ip = resolve_client_ip(self)
        resource_name = path.rsplit("/", 1)[-1]
        resource_uri = f"hello://{resource_name}"
        try:
            payload = build_resource_contents_payload(resource_uri)
        except KeyError:
            self.handle_not_found(path)
            return
        LOGGER.info(
            "request path=%s method=%s status=%s client_ip=%s",
            path,
            self.command,
            HTTPStatus.OK.value,
            client_ip,
        )
        self.send_json(payload, HTTPStatus.OK, "GET, OPTIONS")

    def handle_prompts(self) -> None:
        client_ip = resolve_client_ip(self)
        payload = build_prompts_payload()
        LOGGER.info(
            "request path=/hello/prompts method=%s status=%s client_ip=%s prompt_count=%s",
            self.command,
            HTTPStatus.OK.value,
            client_ip,
            len(payload["prompts"]),
        )
        self.send_json(payload, HTTPStatus.OK, "GET, OPTIONS")

    def handle_prompt(self, parsed_url) -> None:
        client_ip = resolve_client_ip(self)
        prompt_name = parsed_url.path.rsplit("/", 1)[-1]
        query_params = parse_qs(parsed_url.query)
        try:
            payload = build_prompt_details_payload(
                prompt_name,
                name=first_value(query_params, "name"),
                lang=first_value(query_params, "lang"),
            )
        except KeyError:
            self.handle_not_found(parsed_url.path)
            return
        LOGGER.info(
            "request path=%s method=%s status=%s client_ip=%s",
            parsed_url.path,
            self.command,
            HTTPStatus.OK.value,
            client_ip,
        )
        self.send_json(payload, HTTPStatus.OK, "GET, OPTIONS")

    def handle_not_found(self, path: str) -> None:
        LOGGER.warning(
            "request path=%s method=%s status=%s client_ip=%s",
            path,
            self.command,
            HTTPStatus.NOT_FOUND.value,
            self.client_address[0],
        )
        self.send_json({"error": "Not Found"}, HTTPStatus.NOT_FOUND)

    def read_json_body(self) -> dict[str, object] | None:
        content_length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(content_length) if content_length > 0 else b""
        if not body:
            return {}
        try:
            parsed = json.loads(body.decode("utf-8"))
        except json.JSONDecodeError:
            LOGGER.warning(
                "request path=/hello method=%s status=%s client_ip=%s invalid_json=true",
                self.command,
                HTTPStatus.BAD_REQUEST.value,
                resolve_client_ip(self),
            )
            return None
        if not isinstance(parsed, dict):
            LOGGER.warning(
                "request path=/hello method=%s status=%s client_ip=%s invalid_json_type=%s",
                self.command,
                HTTPStatus.BAD_REQUEST.value,
                resolve_client_ip(self),
                type(parsed).__name__,
            )
            return None
        return parsed


def first_value(query_params: dict[str, list[str]], key: str) -> str | None:
    values = query_params.get(key)
    if not values:
        return None
    value = values[0].strip()
    return value or None


def resolve_client_ip(handler: BaseHTTPRequestHandler) -> str:
    return handler.headers.get("X-Forwarded-For", handler.client_address[0]).split(",")[0].strip()


def allowed_methods_for_path(path: str) -> str | None:
    if path == "/hello":
        return ALLOWED_METHODS
    if path in {"/hello/languages", "/hello/resources", "/hello/prompts"}:
        return "GET, OPTIONS"
    if path.startswith("/hello/resources/") or path.startswith("/hello/prompts/"):
        return "GET, OPTIONS"
    return None


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), HelloHandler)
    LOGGER.info("server_start host=%s port=%s log_file=%s", HOST, PORT, LOG_FILE)
    print(f"Python hello API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
