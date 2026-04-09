import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HelloApiServer {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8081;

    private HelloApiServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/hello", new HelloHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Java hello API listening on http://" + HOST + ":" + PORT);
    }

    private static final class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            URI requestUri = exchange.getRequestURI();
            Map<String, String> queryParams = parseQuery(requestUri.getRawQuery());
            String name = queryParams.get("name");
            String lang = queryParams.get("lang");
            String ip = resolveClientIp(exchange);
            Map<String, Object> payload = HelloService.buildHelloPayload(name, lang, ip);
            writeJson(exchange, 200, toJson(payload));
        }
    }

    private static String resolveClientIp(HttpExchange exchange) {
        String forwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return queryParams;
        }

        for (String part : rawQuery.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            String[] pieces = part.split("=", 2);
            String key = decode(pieces[0]);
            String value = pieces.length > 1 ? decode(pieces[1]) : "";
            if (!value.isBlank()) {
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String toJson(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                builder.append(value);
            } else {
                builder.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (char current : value.toCharArray()) {
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }
}
