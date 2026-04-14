from __future__ import annotations

import json
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from openweather_service import (
    build_prompt_details_payload,
    build_prompts_payload,
    build_resource_contents_payload,
    build_resources_payload,
    fetch_current_weather,
    fetch_weather_overview,
)


HOST = os.getenv("OPENWEATHER_API_HOST", "127.0.0.1")
PORT = int(os.getenv("OPENWEATHER_API_PORT", "8100"))
ALLOWED_METHODS = "GET, OPTIONS"
LOGS_DIR = Path(__file__).resolve().parents[3] / "logs"
LOG_FILE = LOGS_DIR / "backend-api-openweather-python.log"


def configure_logger() -> logging.Logger:
    LOGS_DIR.mkdir(parents=True, exist_ok=True)
    logger = logging.getLogger("backend.api_openweather.python")
    if logger.handlers:
        return logger
    logger.setLevel(logging.INFO)
    logger.propagate = False
    file_handler = logging.FileHandler(LOG_FILE, encoding="utf-8")
    file_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
    logger.addHandler(file_handler)
    return logger


LOGGER = configure_logger()


class OpenWeatherHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        if parsed_url.path == "/openweather/current":
            self.handle_current(parsed_url)
            return
        if parsed_url.path == "/openweather/overview":
            self.handle_overview(parsed_url)
            return
        if parsed_url.path == "/openweather/resources":
            self.send_json(build_resources_payload(), HTTPStatus.OK, ALLOWED_METHODS)
            return
        if parsed_url.path.startswith("/openweather/resources/"):
            self.handle_resource(parsed_url.path)
            return
        if parsed_url.path == "/openweather/prompts":
            self.send_json(build_prompts_payload(), HTTPStatus.OK, ALLOWED_METHODS)
            return
        if parsed_url.path.startswith("/openweather/prompts/"):
            self.handle_prompt(parsed_url)
            return
        self.handle_not_found(parsed_url.path)

    def do_OPTIONS(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        allow_methods = allowed_methods_for_path(parsed_url.path)
        if allow_methods is None:
            self.handle_not_found(parsed_url.path)
            return
        self.send_response(HTTPStatus.NO_CONTENT.value)
        self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return

    def handle_current(self, parsed_url) -> None:
        query_params = parse_qs(parsed_url.query)
        query = first_value(query_params, "q")
        if not query:
            self.send_json({"error": "Missing q query parameter"}, HTTPStatus.BAD_REQUEST, ALLOWED_METHODS)
            return
        try:
            payload = fetch_current_weather(
                query=query,
                units=first_value(query_params, "units"),
                lang=first_value(query_params, "lang"),
            )
        except RuntimeError as exception:
            self.send_json({"error": str(exception)}, HTTPStatus.BAD_GATEWAY, ALLOWED_METHODS)
            return
        LOGGER.info("request path=/openweather/current method=%s query=%s", self.command, query)
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_overview(self, parsed_url) -> None:
        query_params = parse_qs(parsed_url.query)
        query = first_value(query_params, "q")
        if not query:
            self.send_json({"error": "Missing q query parameter"}, HTTPStatus.BAD_REQUEST, ALLOWED_METHODS)
            return
        try:
            payload = fetch_weather_overview(
                query=query,
                units=first_value(query_params, "units"),
                date=first_value(query_params, "date"),
            )
        except KeyError:
            self.send_json({"error": "Unsupported query for geocoding"}, HTTPStatus.BAD_REQUEST, ALLOWED_METHODS)
            return
        except RuntimeError as exception:
            self.send_json({"error": str(exception)}, HTTPStatus.BAD_GATEWAY, ALLOWED_METHODS)
            return
        LOGGER.info("request path=/openweather/overview method=%s query=%s", self.command, query)
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_resource(self, path: str) -> None:
        resource_name = path.rsplit("/", 1)[-1]
        try:
            payload = build_resource_contents_payload(f"openweather://{resource_name}")
        except KeyError:
            self.handle_not_found(path)
            return
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_prompt(self, parsed_url) -> None:
        prompt_name = parsed_url.path.rsplit("/", 1)[-1]
        query_params = parse_qs(parsed_url.query)
        try:
            payload = build_prompt_details_payload(
                prompt_name,
                query=first_value(query_params, "query"),
                units=first_value(query_params, "units"),
            )
        except KeyError:
            self.handle_not_found(parsed_url.path)
            return
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_not_found(self, path: str) -> None:
        self.send_json({"error": "Not Found"}, HTTPStatus.NOT_FOUND)

    def send_json(self, payload: object, status: HTTPStatus, allow_methods: str | None = None) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("X-Powered-By", "Python")
        if allow_methods:
            self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def first_value(query_params: dict[str, list[str]], key: str) -> str | None:
    values = query_params.get(key)
    if not values:
        return None
    value = values[0].strip()
    return value or None


def allowed_methods_for_path(path: str) -> str | None:
    if path in {"/openweather/current", "/openweather/overview", "/openweather/resources", "/openweather/prompts"}:
        return ALLOWED_METHODS
    if path.startswith("/openweather/resources/") or path.startswith("/openweather/prompts/"):
        return ALLOWED_METHODS
    return None


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), OpenWeatherHandler)
    LOGGER.info("server_start host=%s port=%s log_file=%s", HOST, PORT, LOG_FILE)
    print(f"Python openweather API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
