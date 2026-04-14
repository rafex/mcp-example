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

public final class OpenWeatherApiServer {
    private static final String HOST = System.getenv().getOrDefault("OPENWEATHER_API_HOST", "127.0.0.1");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("OPENWEATHER_API_PORT", "8101"));
    private static final String ALLOW_HEADER = "GET, OPTIONS";
    private static final Path LOGS_DIR = Path.of(System.getProperty("user.dir"), "logs");
    private static final Path LOG_FILE = LOGS_DIR.resolve("backend-api-openweather-java.log");
    private static final Logger LOGGER = configureLogger();

    private OpenWeatherApiServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/openweather", new OpenWeatherHandler());
        server.setExecutor(null);
        server.start();
        LOGGER.info("server_start host=" + HOST + " port=" + PORT + " log_file=" + LOG_FILE);
        System.out.println("Java openweather API listening on http://" + HOST + ":" + PORT);
    }

    private static final class OpenWeatherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase();
            String allowMethods = allowedMethods(path);
            if (allowMethods == null) {
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
                return;
            }
            if ("OPTIONS".equals(method)) {
                exchange.getResponseHeaders().set("Allow", allowMethods);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equals(method)) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}", allowMethods);
                return;
            }
            if ("/openweather/current".equals(path)) {
                writeCurrent(exchange);
                return;
            }
            if ("/openweather/overview".equals(path)) {
                writeOverview(exchange);
                return;
            }
            if ("/openweather/resources".equals(path)) {
                writeJson(exchange, 200, toJsonValue(OpenWeatherService.buildResourcesPayload()), ALLOW_HEADER);
                return;
            }
            if (path.startsWith("/openweather/resources/")) {
                writeResource(exchange, path);
                return;
            }
            if ("/openweather/prompts".equals(path)) {
                writeJson(exchange, 200, toJsonValue(OpenWeatherService.buildPromptsPayload()), ALLOW_HEADER);
                return;
            }
            if (path.startsWith("/openweather/prompts/")) {
                writePrompt(exchange, path);
                return;
            }
            writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
        }

        private void writeCurrent(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String location = query.get("q");
            if (location == null || location.isBlank()) {
                writeJson(exchange, 400, "{\"error\":\"Missing q query parameter\"}", ALLOW_HEADER);
                return;
            }
            try {
                writeJson(exchange, 200, OpenWeatherService.fetchCurrentWeather(location, query.get("units"), query.get("lang")), ALLOW_HEADER);
            } catch (IllegalStateException exception) {
                writeJson(exchange, 502, "{\"error\":" + quote(exception.getMessage()) + "}", ALLOW_HEADER);
            }
        }

        private void writeOverview(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String location = query.get("q");
            if (location == null || location.isBlank()) {
                writeJson(exchange, 400, "{\"error\":\"Missing q query parameter\"}", ALLOW_HEADER);
                return;
            }
            try {
                writeJson(exchange, 200, OpenWeatherService.fetchWeatherOverview(location, query.get("units"), query.get("date")), ALLOW_HEADER);
            } catch (IllegalArgumentException exception) {
                writeJson(exchange, 400, "{\"error\":" + quote(exception.getMessage()) + "}", ALLOW_HEADER);
            } catch (IllegalStateException exception) {
                writeJson(exchange, 502, "{\"error\":" + quote(exception.getMessage()) + "}", ALLOW_HEADER);
            }
        }

        private void writeResource(HttpExchange exchange, String path) throws IOException {
            String resourceName = path.substring(path.lastIndexOf('/') + 1);
            try {
                writeJson(exchange, 200, toJsonValue(OpenWeatherService.buildResourceContentsPayload("openweather://" + resourceName)), ALLOW_HEADER);
            } catch (IllegalArgumentException exception) {
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }

        private void writePrompt(HttpExchange exchange, String path) throws IOException {
            String promptName = path.substring(path.lastIndexOf('/') + 1);
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                writeJson(exchange, 200, toJsonValue(OpenWeatherService.buildPromptDetailsPayload(promptName, query.get("query"), query.get("units"))), ALLOW_HEADER);
            } catch (IllegalArgumentException exception) {
                writeJson(exchange, 404, "{\"error\":\"Not Found\"}", null);
            }
        }
    }

    private static Logger configureLogger() {
        try {
            Files.createDirectories(LOGS_DIR);
            Logger logger = Logger.getLogger("backend.api_openweather.java");
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

    private static String allowedMethods(String path) {
        if ("/openweather/current".equals(path)
            || "/openweather/overview".equals(path)
            || "/openweather/resources".equals(path)
            || "/openweather/prompts".equals(path)
            || path.startsWith("/openweather/resources/")
            || path.startsWith("/openweather/prompts/")) {
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
            return quote(stringValue);
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
                builder.append(quote(String.valueOf(entry.getKey()))).append(":").append(toJsonValue(entry.getValue()));
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
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class PlainFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%1$tF %1$tT %2$s %3$s%n", record.getMillis(), record.getLevel().getName(), formatMessage(record));
        }
    }
}
