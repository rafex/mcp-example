import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class OpenWeatherMcpServer {
    private static final String PROTOCOL_VERSION = "2024-11-05";

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
            return success(id, OpenWeatherService.buildToolsPayload());
        }
        if ("tools/call".equals(method)) {
            String toolName = extractJsonString(json, "name");
            if ("get_current_weather".equals(toolName)) {
                String payload = OpenWeatherService.fetchCurrentWeather(extractJsonString(json, "query"), extractJsonString(json, "units"), extractJsonString(json, "lang"));
                return toolResult(id, payload);
            }
            if ("get_weather_overview".equals(toolName)) {
                String payload = OpenWeatherService.fetchWeatherOverview(extractJsonString(json, "query"), extractJsonString(json, "units"), extractJsonString(json, "date"));
                return toolResult(id, payload);
            }
            return error(id, -32602, "Herramienta no soportada: " + toolName);
        }
        if ("resources/list".equals(method)) {
            return success(id, OpenWeatherService.buildResourcesPayload());
        }
        if ("resources/read".equals(method)) {
            String uri = extractJsonString(json, "uri");
            try {
                return success(id, OpenWeatherService.buildResourceContentsPayload(uri));
            } catch (IllegalArgumentException exception) {
                return error(id, -32602, "Resource no soportado: " + uri);
            }
        }
        if ("prompts/list".equals(method)) {
            return success(id, OpenWeatherService.buildPromptsPayload());
        }
        if ("prompts/get".equals(method)) {
            String promptName = extractJsonString(json, "name");
            try {
                return success(id, OpenWeatherService.buildPromptDetailsPayload(promptName, extractJsonString(json, "query"), extractJsonString(json, "units")));
            } catch (IllegalArgumentException exception) {
                return error(id, -32602, "Prompt no soportado: " + promptName);
            }
        }
        return error(id, -32601, "Método no encontrado: " + method);
    }

    private static String toolResult(String id, String payloadJson) {
        return success(id, "{\"content\":[{\"type\":\"text\",\"text\":" + quote(payloadJson) + "}],\"structuredContent\":" + payloadJson + ",\"isError\":false}");
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
        int end = start;
        boolean inString = false;
        while (end < json.length()) {
            char current = json.charAt(end);
            if (current == '"' && (end == start || json.charAt(end - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString && (current == ',' || current == '}' || current == ']')) {
                break;
            }
            end++;
        }
        return json.substring(start, end).trim();
    }
}
