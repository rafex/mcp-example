import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
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

public final class HelloApiServer {
    private static final String HOST = System.getenv().getOrDefault("HELLO_API_HOST", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("HELLO_API_PORT", "8081"));
    private static final String ALLOW_HEADER = "GET, POST, OPTIONS";
    private static final Path LOGS_DIR = Path.of(System.getProperty("user.dir"), "logs");
    private static final Path LOG_FILE = LOGS_DIR.resolve("backend-api-hello-java.log");
    private static final Logger LOGGER = configureLogger();

    private HelloApiServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/hello", new HelloHandler());
        server.setExecutor(null);
        server.start();
        LOGGER.info("server_start host=" + HOST + " port=" + PORT + " log_file=" + LOG_FILE);
        System.out.println("Java hello API listening on http://" + HOST + ":" + PORT);
    }

    private static final class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();

            if (allowedMethods(path) == null) {
                LOGGER.warning("request path=" + path + " method=" + method + " status=404 client_ip=" + resolveClientIp(exchange));
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
                return;
            }

            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "OPTIONS" -> handleOptions(exchange);
                default -> {
                    LOGGER.warning("request path=" + path + " method=" + method + " status=405 client_ip=" + resolveClientIp(exchange));
                    writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}", allowedMethods(path));
                }
            }
        }

        private void handleGet(HttpExchange exchange, String path) throws IOException {
            if ("/hello/languages".equals(path)) {
                writeLanguages(exchange);
                return;
            }
            if ("/hello/resources".equals(path)) {
                writeResources(exchange);
                return;
            }
            if (path.startsWith("/hello/resources/")) {
                writeResource(exchange, path);
                return;
            }
            if ("/hello/prompts".equals(path)) {
                writePrompts(exchange);
                return;
            }
            if (path.startsWith("/hello/prompts/")) {
                writePrompt(exchange, path);
                return;
            }
            URI requestUri = exchange.getRequestURI();
            Map<String, String> queryParams = parseQuery(requestUri.getRawQuery());
            writeHello(exchange, queryParams.get("name"), queryParams.get("lang"));
        }

        private void handlePost(HttpExchange exchange, String path) throws IOException {
            if (!"/hello".equals(path)) {
                LOGGER.warning("request path=" + path + " method=POST status=405 client_ip=" + resolveClientIp(exchange));
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}", allowedMethods(path));
                return;
            }
            String body = readRequestBody(exchange.getRequestBody());
            Map<String, String> requestBody = parseSimpleJsonObject(body);
            if (requestBody == null) {
                LOGGER.warning("request path=/hello method=POST status=400 client_ip=" + resolveClientIp(exchange) + " invalid_json=true");
                writeJson(exchange, 400, "{\"error\":\"Invalid JSON body\"}", ALLOW_HEADER);
                return;
            }
            writeHello(exchange, requestBody.get("name"), requestBody.get("lang"));
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            String ip = resolveClientIp(exchange);
            LOGGER.info("request path=" + exchange.getRequestURI().getPath() + " method=OPTIONS status=204 client_ip=" + ip);
            exchange.getResponseHeaders().set("Allow", allowedMethods(exchange.getRequestURI().getPath()));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private void writeHello(HttpExchange exchange, String name, String lang) throws IOException {
            String ip = resolveClientIp(exchange);
            Map<String, Object> payload = HelloService.buildHelloPayload(name, lang, ip);
            LOGGER.info(
                "request path=/hello method=" + exchange.getRequestMethod().toUpperCase()
                    + " status=200 client_ip=" + ip
                    + " lang=" + payload.get("lang")
                    + " has_name=" + payload.get("has_name")
            );
            writeJson(exchange, 200, toJsonValue(payload), ALLOW_HEADER);
        }

        private void writeLanguages(HttpExchange exchange) throws IOException {
            String ip = resolveClientIp(exchange);
            Map<String, Object> payload = HelloService.buildLanguagesPayload();
            LOGGER.info(
                "request path=/hello/languages method=" + exchange.getRequestMethod().toUpperCase()
                    + " status=200 client_ip=" + ip
                    + " language_count=" + payload.get("language_count")
            );
            writeJson(exchange, 200, toJsonValue(payload), "GET, OPTIONS");
        }

        private void writeResources(HttpExchange exchange) throws IOException {
            String ip = resolveClientIp(exchange);
            Map<String, Object> payload = HelloService.buildResourcesPayload();
            LOGGER.info(
                "request path=/hello/resources method=" + exchange.getRequestMethod().toUpperCase()
                    + " status=200 client_ip=" + ip
                    + " resource_count=" + ((java.util.List<?>) payload.get("resources")).size()
            );
            writeJson(exchange, 200, toJsonValue(payload), "GET, OPTIONS");
        }

        private void writeResource(HttpExchange exchange, String path) throws IOException {
            String ip = resolveClientIp(exchange);
            String resourceName = path.substring(path.lastIndexOf('/') + 1);
            String resourceUri = "hello://" + resourceName;
            try {
                Map<String, Object> payload = HelloService.buildResourceContentsPayload(resourceUri);
                LOGGER.info(
                    "request path=" + path + " method=" + exchange.getRequestMethod().toUpperCase()
                        + " status=200 client_ip=" + ip
                );
                writeJson(exchange, 200, toJsonValue(payload), "GET, OPTIONS");
            } catch (IllegalArgumentException exception) {
                LOGGER.warning("request path=" + path + " method=" + exchange.getRequestMethod().toUpperCase() + " status=404 client_ip=" + ip);
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }

        private void writePrompts(HttpExchange exchange) throws IOException {
            String ip = resolveClientIp(exchange);
            Map<String, Object> payload = HelloService.buildPromptsPayload();
            LOGGER.info(
                "request path=/hello/prompts method=" + exchange.getRequestMethod().toUpperCase()
                    + " status=200 client_ip=" + ip
            );
            writeJson(exchange, 200, toJsonValue(payload), "GET, OPTIONS");
        }

        private void writePrompt(HttpExchange exchange, String path) throws IOException {
            String ip = resolveClientIp(exchange);
            String promptName = path.substring(path.lastIndexOf('/') + 1);
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                Map<String, Object> payload = HelloService.buildPromptDetailsPayload(
                    promptName,
                    queryParams.get("name"),
                    queryParams.get("lang")
                );
                LOGGER.info(
                    "request path=" + path + " method=" + exchange.getRequestMethod().toUpperCase()
                        + " status=200 client_ip=" + ip
                );
                writeJson(exchange, 200, toJsonValue(payload), "GET, OPTIONS");
            } catch (IllegalArgumentException exception) {
                LOGGER.warning("request path=" + path + " method=" + exchange.getRequestMethod().toUpperCase() + " status=404 client_ip=" + ip);
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }
    }

    private static Logger configureLogger() {
        try {
            Files.createDirectories(LOGS_DIR);
            Logger logger = Logger.getLogger("backend.api_hello.java");
            logger.setUseParentHandlers(false);
            if (logger.getHandlers().length == 0) {
                FileHandler fileHandler = new FileHandler(LOG_FILE.toString(), true);
                fileHandler.setEncoding(StandardCharsets.UTF_8.name());
                fileHandler.setFormatter(new PlainFormatter());
                logger.addHandler(fileHandler);
            }
            logger.setLevel(Level.INFO);
            return logger;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to configure logger at " + LOG_FILE, exception);
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

    private static void writeJson(HttpExchange exchange, int statusCode, String json, String allowHeader) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (allowHeader != null) {
            exchange.getResponseHeaders().set("Allow", allowHeader);
        }
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String readRequestBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseSimpleJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return new LinkedHashMap<>();
        }

        String trimmed = body.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null;
        }

        Map<String, String> values = new LinkedHashMap<>();
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return values;
        }

        for (String entry : splitJsonEntries(content)) {
            String[] pieces = entry.split(":", 2);
            if (pieces.length != 2) {
                return null;
            }
            String key = unquote(pieces[0].trim());
            String rawValue = pieces[1].trim();
            if (key == null) {
                return null;
            }
            if ("null".equals(rawValue)) {
                values.put(key, null);
                continue;
            }
            String value = unquote(rawValue);
            if (value == null) {
                return null;
            }
            values.put(key, value);
        }

        return values;
    }

    private static String[] splitJsonEntries(String content) {
        java.util.List<String> entries = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (char ch : content.toCharArray()) {
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                current.append(ch);
                inString = !inString;
                continue;
            }
            if (ch == ',' && !inString) {
                entries.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        entries.add(current.toString().trim());
        return entries.toArray(String[]::new);
    }

    private static String unquote(String value) {
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            return null;
        }
        String inner = value.substring(1, value.length() - 1);
        return inner
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\");
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

    private static String allowedMethods(String path) {
        if ("/hello".equals(path)) {
            return ALLOW_HEADER;
        }
        if ("/hello/languages".equals(path)
            || "/hello/resources".equals(path)
            || "/hello/prompts".equals(path)
            || path.startsWith("/hello/resources/")
            || path.startsWith("/hello/prompts/")) {
            return "GET, OPTIONS";
        }
        return null;
    }

    private static final class PlainFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format(
                "%1$tF %1$tT %2$s %3$s%n",
                record.getMillis(),
                record.getLevel().getName(),
                formatMessage(record)
            );
        }
    }
}
