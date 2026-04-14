import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class OpenWeatherMcpServer {
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String OPENWEATHER_API_BASE_URL =
        System.getenv().getOrDefault("OPENWEATHER_API_BASE_URL", "http://127.0.0.1:8101");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private OpenWeatherMcpServer() {
    }

    public static void main(String[] args) throws IOException {
        BufferedInputStream input = new BufferedInputStream(System.in);
        while (true) {
            String message = readMessage(input);
            if (message == null) {
                return;
            }
            String response = handleRequest(message);
            if (response != null) {
                writeMessage(response);
            }
        }
    }

    private static String handleRequest(String json) {
        String method = extractJsonString(json, "method");
        String id = extractJsonRaw(json, "id");

        if ("notifications/initialized".equals(method)) {
            return null;
        }
        if ("initialize".equals(method)) {
            return success(id, "{\"protocolVersion\":\"" + PROTOCOL_VERSION + "\",\"capabilities\":{\"tools\":{},\"resources\":{},\"prompts\":{}},\"serverInfo\":{\"name\":\"openweather-java-mcp\",\"version\":\"0.1.0\"}}");
        }
        if ("tools/list".equals(method)) {
            return success(
                id,
                "{"
                    + "\"tools\":["
                    + "{"
                    + "\"name\":\"get_current_weather\","
                    + "\"description\":\"Consulta el clima actual usando OpenWeather Current Weather API 2.5.\","
                    + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"Ciudad o ciudad,country code. Ejemplo: London,uk\"},\"units\":{\"type\":\"string\",\"description\":\"standard, metric o imperial\"},\"lang\":{\"type\":\"string\",\"description\":\"Idioma del campo weather.description\"}},\"required\":[\"query\"],\"additionalProperties\":false}"
                    + "},"
                    + "{"
                    + "\"name\":\"get_weather_overview\","
                    + "\"description\":\"Obtiene un resumen legible usando One Call API 3.0 overview.\","
                    + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"Ciudad o ciudad,country code. Ejemplo: London,uk\"},\"units\":{\"type\":\"string\",\"description\":\"standard, metric o imperial\"},\"date\":{\"type\":\"string\",\"description\":\"Fecha opcional YYYY-MM-DD, hoy o mañana\"}},\"required\":[\"query\"],\"additionalProperties\":false}"
                    + "}"
                    + "]"
                    + "}"
            );
        }
        if ("tools/call".equals(method)) {
            String toolName = extractJsonString(json, "name");
            if ("get_current_weather".equals(toolName)) {
                String payload = callApi("/openweather/current", mapQuery("q", extractJsonString(json, "query"), "units", extractJsonString(json, "units"), "lang", extractJsonString(json, "lang")));
                return toolResult(id, payload);
            }
            if ("get_weather_overview".equals(toolName)) {
                String payload = callApi("/openweather/overview", mapQuery("q", extractJsonString(json, "query"), "units", extractJsonString(json, "units"), "date", extractJsonString(json, "date")));
                return toolResult(id, payload);
            }
            return error(id, -32602, "Herramienta no soportada: " + toolName);
        }
        if ("resources/list".equals(method)) {
            return success(id, callApi("/openweather/resources", new String[0]));
        }
        if ("resources/read".equals(method)) {
            String uri = extractJsonString(json, "uri");
            String path = resourcePath(uri);
            if (path == null) {
                return error(id, -32602, "Resource no soportado: " + uri);
            }
            return success(id, callApi(path, new String[0]));
        }
        if ("prompts/list".equals(method)) {
            return success(id, callApi("/openweather/prompts", new String[0]));
        }
        if ("prompts/get".equals(method)) {
            String promptName = extractJsonString(json, "name");
            String path = promptPath(promptName, extractJsonString(json, "query"), extractJsonString(json, "units"));
            if (path == null) {
                return error(id, -32602, "Prompt no soportado: " + promptName);
            }
            return success(id, callApi(path, new String[0]));
        }
        return error(id, -32601, "Método no encontrado: " + method);
    }

    private static String toolResult(String id, String payloadJson) {
        return success(id, "{\"content\":[{\"type\":\"text\",\"text\":" + quote(payloadJson) + "}],\"structuredContent\":" + payloadJson + ",\"isError\":false}");
    }

    private static String resourcePath(String resourceUri) {
        if ("openweather://service-overview".equals(resourceUri)) {
            return "/openweather/resources/service-overview";
        }
        if ("openweather://unit-reference".equals(resourceUri)) {
            return "/openweather/resources/unit-reference";
        }
        return null;
    }

    private static String promptPath(String promptName, String query, String units) {
        if ("current-weather-brief".equals(promptName)) {
            return buildPath("/openweather/prompts/current-weather-brief", mapQuery("query", query, "units", units));
        }
        if ("weather-overview-brief".equals(promptName)) {
            return buildPath("/openweather/prompts/weather-overview-brief", mapQuery("query", query, "units", units));
        }
        return null;
    }

    private static String[] mapQuery(String... values) {
        return values;
    }

    private static String buildPath(String path, String[] queryPairs) {
        StringBuilder builder = new StringBuilder(path);
        boolean first = true;
        for (int index = 0; index < queryPairs.length; index += 2) {
            String key = queryPairs[index];
            String value = queryPairs[index + 1];
            if (value == null || value.isBlank()) {
                continue;
            }
            builder.append(first ? "?" : "&");
            first = false;
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static String callApi(String path, String[] queryPairs) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OPENWEATHER_API_BASE_URL + buildPath(path, queryPairs))).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("OpenWeather backend responded with HTTP " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenWeather backend unavailable at " + OPENWEATHER_API_BASE_URL, exception);
        }
    }

    private static String readMessage(BufferedInputStream input) throws IOException {
        Integer contentLength = null;
        while (true) {
            String line = readHeaderLine(input);
            if (line == null) {
                return null;
            }
            if (line.isEmpty()) {
                break;
            }
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }
        if (contentLength == null) {
            return null;
        }
        byte[] body = input.readNBytes(contentLength);
        if (body.length == 0) {
            return null;
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static String readHeaderLine(BufferedInputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int next = input.read();
            if (next == -1) {
                if (builder.isEmpty()) {
                    return null;
                }
                throw new EOFException("Unexpected EOF while reading MCP header");
            }
            if (next == '\r') {
                int following = input.read();
                if (following == '\n') {
                    return builder.toString();
                }
                if (following != -1) {
                    builder.append((char) next).append((char) following);
                }
                continue;
            }
            if (next == '\n') {
                return builder.toString();
            }
            builder.append((char) next);
        }
    }

    private static void writeMessage(String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + body.length + "\r\n\r\n";
        System.out.write(header.getBytes(StandardCharsets.UTF_8));
        System.out.write(body);
        System.out.flush();
    }

    private static String success(String id, String resultJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + normalizeId(id) + ",\"result\":" + resultJson + "}";
    }

    private static String error(String id, int code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + normalizeId(id) + ",\"error\":{\"code\":" + code + ",\"message\":" + quote(message) + "}}";
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return "null";
        }
        return id;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String extractJsonString(String json, String key) {
        String keyPattern = "\"" + key + "\"";
        int foundAt = json.indexOf(keyPattern);
        if (foundAt < 0) {
            return null;
        }
        int colon = json.indexOf(':', foundAt);
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = start + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"' && json.charAt(index - 1) != '\\') {
                return builder.toString().replace("\\\"", "\"").replace("\\\\", "\\");
            }
            builder.append(current);
        }
        return null;
    }

    private static String extractJsonRaw(String json, String key) {
        String pattern = "\"" + key + "\"";
        int foundAt = json.indexOf(pattern);
        if (foundAt < 0) {
            return null;
        }
        int colon = json.indexOf(':', foundAt);
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && ",}\r\n".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end).trim();
    }
}
