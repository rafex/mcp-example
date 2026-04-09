from __future__ import annotations

import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from hello_service import build_hello_payload


HOST = "127.0.0.1"
PORT = 8080


class HelloHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        parsed_url = urlparse(self.path)
        if parsed_url.path != "/hello":
            self.send_json({"error": "Not Found"}, HTTPStatus.NOT_FOUND)
            return

        query_params = parse_qs(parsed_url.query)
        name = first_value(query_params, "name")
        lang = first_value(query_params, "lang")
        client_ip = self.headers.get("X-Forwarded-For", self.client_address[0]).split(",")[0].strip()
        payload = build_hello_payload(name=name, lang=lang, ip=client_ip)
        self.send_json(payload, HTTPStatus.OK)

    def log_message(self, format: str, *args: object) -> None:
        return

    def send_json(self, payload: dict[str, object], status: HTTPStatus) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def first_value(query_params: dict[str, list[str]], key: str) -> str | None:
    values = query_params.get(key)
    if not values:
        return None
    value = values[0].strip()
    return value or None


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), HelloHandler)
    print(f"Python hello API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
