from __future__ import annotations

import json
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from date_service import (
    build_locations_payload,
    build_prompt_details_payload,
    build_prompts_payload,
    build_resource_contents_payload,
    build_resources_payload,
    build_time_payload,
    normalize_location,
)


HOST = os.getenv("DATE_API_HOST", "127.0.0.1")
PORT = int(os.getenv("DATE_API_PORT", "8090"))
DATE_API_TOKEN = os.getenv("DATE_API_TOKEN", "dev-date-token")
DATE_API_CLIENT_ID = os.getenv("DATE_API_CLIENT_ID", "mcp-date-client")
ALLOWED_METHODS = "GET, OPTIONS"
LOGS_DIR = Path(__file__).resolve().parents[3] / "logs"
LOG_FILE = LOGS_DIR / "backend-api-date-python.log"


def configure_logger() -> logging.Logger:
    LOGS_DIR.mkdir(parents=True, exist_ok=True)
    logger = logging.getLogger("backend.api_date.python")
    if logger.handlers:
        return logger
    logger.setLevel(logging.INFO)
    logger.propagate = False
    file_handler = logging.FileHandler(LOG_FILE, encoding="utf-8")
    file_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
    logger.addHandler(file_handler)
    return logger


LOGGER = configure_logger()


class DateHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        if not require_auth(self):
            return

        if parsed_url.path == "/date/time":
            self.handle_time(parsed_url)
            return
        if parsed_url.path == "/date/locations":
            self.handle_locations()
            return
        if parsed_url.path == "/date/resources":
            self.handle_resources()
            return
        if parsed_url.path.startswith("/date/resources/"):
            self.handle_resource(parsed_url.path)
            return
        if parsed_url.path == "/date/prompts":
            self.handle_prompts()
            return
        if parsed_url.path.startswith("/date/prompts/"):
            self.handle_prompt(parsed_url)
            return
        self.handle_not_found(parsed_url.path)

    def do_OPTIONS(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        allow_methods = allowed_methods_for_path(parsed_url.path)
        if allow_methods is None:
            self.handle_not_found(parsed_url.path)
            return

        LOGGER.info(
            "request path=%s method=OPTIONS status=%s client_ip=%s",
            parsed_url.path,
            HTTPStatus.NO_CONTENT.value,
            resolve_client_ip(self),
        )
        self.send_response(HTTPStatus.NO_CONTENT.value)
        self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return

    def handle_time(self, parsed_url) -> None:
        query_params = parse_qs(parsed_url.query)
        location = first_value(query_params, "location")
        try:
            payload = build_time_payload(location or "mx-central", resolve_client_ip(self))
        except KeyError:
            self.send_json({"error": "Unsupported location"}, HTTPStatus.BAD_REQUEST, ALLOWED_METHODS)
            return
        LOGGER.info(
            "request path=/date/time method=%s status=%s client_ip=%s location=%s",
            self.command,
            HTTPStatus.OK.value,
            payload["ip"],
            payload["location"],
        )
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_locations(self) -> None:
        payload = build_locations_payload()
        LOGGER.info(
            "request path=/date/locations method=%s status=%s client_ip=%s location_count=%s",
            self.command,
            HTTPStatus.OK.value,
            resolve_client_ip(self),
            payload["location_count"],
        )
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_resources(self) -> None:
        payload = build_resources_payload()
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_resource(self, path: str) -> None:
        resource_name = path.rsplit("/", 1)[-1]
        resource_uri = f"date://{resource_name}"
        try:
            payload = build_resource_contents_payload(resource_uri)
        except KeyError:
            self.handle_not_found(path)
            return
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_prompts(self) -> None:
        payload = build_prompts_payload()
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_prompt(self, parsed_url) -> None:
        prompt_name = parsed_url.path.rsplit("/", 1)[-1]
        query_params = parse_qs(parsed_url.query)
        try:
            payload = build_prompt_details_payload(
                prompt_name,
                from_location=first_value(query_params, "from_location") or first_value(query_params, "location"),
                to_location=first_value(query_params, "to_location"),
            )
        except KeyError:
            self.handle_not_found(parsed_url.path)
            return
        self.send_json(payload, HTTPStatus.OK, ALLOWED_METHODS)

    def handle_not_found(self, path: str) -> None:
        LOGGER.warning(
            "request path=%s method=%s status=%s client_ip=%s",
            path,
            self.command,
            HTTPStatus.NOT_FOUND.value,
            resolve_client_ip(self),
        )
        self.send_json({"error": "Not Found"}, HTTPStatus.NOT_FOUND)

    def send_json(self, payload: dict[str, object], status: HTTPStatus, allow_methods: str | None = None) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("X-Powered-By", "Python")
        if allow_methods:
            self.send_header("Allow", allow_methods)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def require_auth(handler: DateHandler) -> bool:
    authorization = handler.headers.get("Authorization", "")
    client_id = handler.headers.get("X-Date-Client", "")
    expected = f"Bearer {DATE_API_TOKEN}"
    if authorization != expected or client_id != DATE_API_CLIENT_ID:
        LOGGER.warning(
            "request path=%s method=%s status=%s client_ip=%s auth_failed=true",
            handler.path,
            handler.command,
            HTTPStatus.UNAUTHORIZED.value,
            resolve_client_ip(handler),
        )
        handler.send_json({"error": "Unauthorized"}, HTTPStatus.UNAUTHORIZED, allowed_methods_for_path(urlparse(handler.path).path))
        return False
    return True


def allowed_methods_for_path(path: str) -> str | None:
    if path in {
        "/date/time",
        "/date/locations",
        "/date/resources",
        "/date/prompts",
    }:
        return ALLOWED_METHODS
    if path.startswith("/date/resources/") or path.startswith("/date/prompts/"):
        return ALLOWED_METHODS
    return None


def resolve_client_ip(handler: BaseHTTPRequestHandler) -> str:
    return handler.headers.get("X-Forwarded-For", handler.client_address[0]).split(",")[0].strip()


def first_value(query_params: dict[str, list[str]], key: str) -> str | None:
    values = query_params.get(key)
    if not values:
        return None
    value = values[0].strip()
    return value or None


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), DateHandler)
    LOGGER.info("server_start host=%s port=%s log_file=%s", HOST, PORT, LOG_FILE)
    print(f"Python date API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
