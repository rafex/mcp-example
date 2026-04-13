import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class DateApiServer {
    private static final String HOST = System.getenv().getOrDefault("DATE_API_HOST", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("DATE_API_PORT", "8091"));
    private static final String DATE_API_TOKEN = System.getenv().getOrDefault("DATE_API_TOKEN", "dev-date-token");
    private static final String DATE_API_CLIENT_ID = System.getenv().getOrDefault("DATE_API_CLIENT_ID", "mcp-date-client");
    private static final String ALLOW_HEADER = "GET, OPTIONS";
    private static final Path LOGS_DIR = Path.of(System.getProperty("user.dir"), "logs");
    private static final Path LOG_FILE = LOGS_DIR.resolve("backend-api-date-java.log");
    private static final Logger LOGGER = configureLogger();

    private DateApiServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/date", new DateHandler());
        server.setExecutor(null);
        server.start();
        LOGGER.info("server_start host=" + HOST + " port=" + PORT + " log_file=" + LOG_FILE);
        System.out.println("Java date API listening on http://" + HOST + ":" + PORT);
    }

    private static final class DateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String allowMethods = allowedMethods(path);

            if (allowMethods == null) {
                logRequest(exchange, 404, path, null, false);
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
                return;
            }

            if ("OPTIONS".equals(method)) {
                logRequest(exchange, 204, path, null, false);
                exchange.getResponseHeaders().set("Allow", allowMethods);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!"GET".equals(method)) {
                logRequest(exchange, 405, path, null, false);
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}", allowMethods);
                return;
            }

            if (!isAuthorized(exchange)) {
                logRequest(exchange, 401, path, null, true);
                writeJson(exchange, 401, "{\"error\":\"Unauthorized\"}", allowMethods);
                return;
            }

            if ("/date/time".equals(path)) {
                writeTime(exchange);
                return;
            }
            if ("/date/locations".equals(path)) {
                logRequest(exchange, 200, "/date/locations", null, false);
                writeJson(exchange, 200, toJsonValue(DateService.buildLocationsPayload()), ALLOW_HEADER);
                return;
            }
            if ("/date/resources".equals(path)) {
                logRequest(exchange, 200, "/date/resources", null, false);
                writeJson(exchange, 200, toJsonValue(DateService.buildResourcesPayload()), ALLOW_HEADER);
                return;
            }
            if (path.startsWith("/date/resources/")) {
                writeResource(exchange, path);
                return;
            }
            if ("/date/prompts".equals(path)) {
                logRequest(exchange, 200, "/date/prompts", null, false);
                writeJson(exchange, 200, toJsonValue(DateService.buildPromptsPayload()), ALLOW_HEADER);
                return;
            }
            if (path.startsWith("/date/prompts/")) {
                writePrompt(exchange, path);
                return;
            }
            writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
        }

        private void writeTime(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String location = DateService.normalizeLocation(queryParams.get("location"));
                logRequest(exchange, 200, "/date/time", location, false);
                writeJson(exchange, 200, toJsonValue(DateService.buildTimePayload(location, resolveClientIp(exchange))), ALLOW_HEADER);
            } catch (IllegalArgumentException exception) {
                logRequest(exchange, 400, "/date/time", queryParams.get("location"), false);
                writeJson(exchange, 400, "{\"error\":\"Unsupported location\"}", ALLOW_HEADER);
            }
        }

        private void writeResource(HttpExchange exchange, String path) throws IOException {
            String resourceName = path.substring(path.lastIndexOf('/') + 1);
            try {
                logRequest(exchange, 200, path, resourceName, false);
                writeJson(exchange, 200, toJsonValue(DateService.buildResourceContentsPayload("date://" + resourceName)), ALLOW_HEADER);
            } catch (IllegalArgumentException exception) {
                logRequest(exchange, 404, path, resourceName, false);
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }

        private void writePrompt(HttpExchange exchange, String path) throws IOException {
            String promptName = path.substring(path.lastIndexOf('/') + 1);
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                logRequest(exchange, 200, path, promptName, false);
                writeJson(
                    exchange,
                    200,
                    toJsonValue(DateService.buildPromptDetailsPayload(
                        promptName,
                        queryParams.getOrDefault("from_location", queryParams.get("location")),
                        queryParams.get("to_location")
                    )),
                    ALLOW_HEADER
                );
            } catch (IllegalArgumentException exception) {
                logRequest(exchange, 404, path, promptName, false);
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }
    }

    private static Logger configureLogger() {
        try {
            Files.createDirectories(LOGS_DIR);
            Logger logger = Logger.getLogger("backend.api_date.java");
            logger.setUseParentHandlers(false);
            if (logger.getHandlers().length == 0) {
                FileHandler handler = new FileHandler(LOG_FILE.toString(), true);
                handler.setEncoding(StandardCharsets.UTF_8.name());
                handler.setFormatter(new PlainFormatter());
                logger.addHandler(handler);
            }
            logger.setLevel(Level.INFO);
            return logger;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        String client = exchange.getRequestHeaders().getFirst("X-Date-Client");
        return ("Bearer " + DATE_API_TOKEN).equals(authorization) && DATE_API_CLIENT_ID.equals(client);
    }

    private static void logRequest(HttpExchange exchange, int statusCode, String path, String detail, boolean authFailed) {
        StringBuilder message = new StringBuilder()
            .append("request path=").append(path)
            .append(" method=").append(exchange.getRequestMethod())
            .append(" status=").append(statusCode)
            .append(" client_ip=").append(resolveClientIp(exchange));
        if (detail != null && !detail.isBlank()) {
            message.append(" detail=").append(detail);
        }
        if (authFailed) {
            message.append(" auth_failed=true");
        }
        LOGGER.info(message.toString());
    }

    private static String resolveClientIp(HttpExchange exchange) {
        String forwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String allowedMethods(String path) {
        if ("/date/time".equals(path)
            || "/date/locations".equals(path)
            || "/date/resources".equals(path)
            || "/date/prompts".equals(path)
            || path.startsWith("/date/resources/")
            || path.startsWith("/date/prompts/")) {
            return ALLOW_HEADER;
        }
        return null;
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
            String key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8);
            String value = pieces.length > 1 ? URLDecoder.decode(pieces[1], StandardCharsets.UTF_8) : "";
            if (!value.isBlank()) {
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String json, String allowHeader) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Powered-By", "Java");
        if (allowHeader != null) {
            exchange.getResponseHeaders().set("Allow", allowHeader);
        }
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                builder.append(toJsonValue(entry.getValue()));
            }
            builder.append("}");
            return builder.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(toJsonValue(item));
            }
            builder.append("]");
            return builder.toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
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

    private static final class PlainFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%1$tF %1$tT %2$s %3$s%n", record.getMillis(), record.getLevel().getName(), formatMessage(record));
        }
    }
}
