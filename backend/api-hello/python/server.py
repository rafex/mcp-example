from __future__ import annotations

import json
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from hello_service import build_hello_payload, build_languages_payload


HOST = os.getenv("HELLO_API_HOST", "127.0.0.1")
PORT = 8080
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

        self.handle_not_found(parsed_url.path)

    def do_POST(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        if parsed_url.path != "/hello":
            self.handle_not_found(parsed_url.path)
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
        if parsed_url.path not in {"/hello", "/hello/languages"}:
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
        self.send_header("Allow", ALLOWED_METHODS)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return

    def send_json(self, payload: dict[str, object], status: HTTPStatus) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Allow", ALLOWED_METHODS)
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
        self.send_json(payload, HTTPStatus.OK)

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
        self.send_json(payload, HTTPStatus.OK)

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


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), HelloHandler)
    LOGGER.info("server_start host=%s port=%s log_file=%s", HOST, PORT, LOG_FILE)
    print(f"Python hello API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
