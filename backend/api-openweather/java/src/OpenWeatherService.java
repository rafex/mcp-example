import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenWeatherService {
    private static final String BASE_URL = System.getenv().getOrDefault("OPENWEATHER_BASE_URL", "https://api.openweathermap.org");
    private static final String API_KEY = System.getenv().getOrDefault("OPENWEATHER_API_KEY", "");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private OpenWeatherService() {
    }

    public static String fetchCurrentWeather(String query, String units, String lang) {
        return callJson("/data/2.5/weather", mapOf(
            "q", query,
            "units", normalizeUnits(units),
            "lang", lang == null || lang.isBlank() ? "en" : lang
        ));
    }

    public static String fetchWeatherOverview(String query, String units, String date) {
        Coordinates coordinates = geocode(query);
        String response = callJson("/data/3.0/onecall/overview", mapOf(
            "lat", coordinates.lat(),
            "lon", coordinates.lon(),
            "units", normalizeUnits(units),
            "date", date
        ));
        return mergeResolvedLocation(response, coordinates, query);
    }

    public static Map<String, Object> buildResourcesPayload() {
        return Map.of(
            "resources",
            List.of(
                resource("openweather://service-overview", "service-overview", "Resumen de los endpoints del wrapper OpenWeatherMap."),
                resource("openweather://unit-reference", "unit-reference", "Referencia de unidades soportadas por OpenWeatherMap.")
            )
        );
    }

    public static Map<String, Object> buildResourceContentsPayload(String resourceUri) {
        if ("openweather://service-overview".equals(resourceUri)) {
            return Map.of(
                "contents",
                List.of(Map.of(
                    "uri", resourceUri,
                    "mimeType", "text/plain",
                    "text", String.join("\n",
                        "OpenWeather wrapper overview",
                        "- GET /openweather/current?q=<city,country>",
                        "- GET /openweather/overview?q=<city,country>",
                        "- GET /openweather/resources",
                        "- GET /openweather/prompts",
                        "- Uses Current Weather API 2.5",
                        "- Uses One Call API 3.0 overview"
                    )
                ))
            );
        }
        if ("openweather://unit-reference".equals(resourceUri)) {
            return Map.of(
                "contents",
                List.of(Map.of(
                    "uri", resourceUri,
                    "mimeType", "text/plain",
                    "text", String.join("\n",
                        "Supported units",
                        "- standard: Kelvin, meter/sec",
                        "- metric: Celsius, meter/sec",
                        "- imperial: Fahrenheit, miles/hour"
                    )
                ))
            );
        }
        throw new IllegalArgumentException("Unsupported resource URI: " + resourceUri);
    }

    public static Map<String, Object> buildPromptsPayload() {
        return Map.of(
            "prompts",
            List.of(
                prompt(
                    "current-weather-brief",
                    "Pide consultar el clima actual de una ciudad con get_current_weather.",
                    List.of(
                        argument("query", "Ciudad o ciudad,country code. Ejemplo: London,uk", true),
                        argument("units", "standard, metric o imperial", false)
                    )
                ),
                prompt(
                    "weather-overview-brief",
                    "Pide un resumen legible del tiempo usando get_weather_overview.",
                    List.of(
                        argument("query", "Ciudad o ciudad,country code. Ejemplo: London,uk", true),
                        argument("units", "standard, metric o imperial", false)
                    )
                )
            )
        );
    }

    public static Map<String, Object> buildPromptDetailsPayload(String promptName, String query, String units) {
        String resolvedQuery = (query == null || query.isBlank()) ? "London,uk" : query;
        String resolvedUnits = normalizeUnits(units);

        if ("current-weather-brief".equals(promptName)) {
            return Map.of(
                "description", "Prompt para consultar el clima actual de una ubicación con OpenWeatherMap.",
                "messages", List.of(
                    message("system", "Usa la herramienta get_current_weather y resume el resultado sin inventar datos."),
                    message("user", "Consulta el clima actual para " + resolvedQuery + " con unidades " + resolvedUnits + " usando get_current_weather.")
                )
            );
        }

        if ("weather-overview-brief".equals(promptName)) {
            return Map.of(
                "description", "Prompt para obtener un resumen legible del tiempo con OpenWeather overview.",
                "messages", List.of(
                    message("system", "Usa la herramienta get_weather_overview y responde con el resumen devuelto."),
                    message("user", "Obtén el weather overview para " + resolvedQuery + " con unidades " + resolvedUnits + " usando get_weather_overview.")
                )
            );
        }

        throw new IllegalArgumentException("Unsupported prompt: " + promptName);
    }

    private static Coordinates geocode(String query) {
        String response = callJson("/geo/1.0/direct", mapOf("q", query, "limit", "1"));
        if (!response.startsWith("[") || response.length() < 3) {
            throw new IllegalArgumentException("Unsupported query for geocoding: " + query);
        }
        String lat = extractJsonNumber(response, "\"lat\"");
        String lon = extractJsonNumber(response, "\"lon\"");
        String name = extractJsonString(response, "\"name\"");
        String country = extractJsonString(response, "\"country\"");
        String state = extractJsonString(response, "\"state\"");
        if (lat == null || lon == null || name == null || country == null) {
            throw new IllegalArgumentException("Unsupported query for geocoding: " + query);
        }
        return new Coordinates(lat, lon, name, country, state);
    }

    private static String mergeResolvedLocation(String json, Coordinates coordinates, String query) {
        String insertion = "\"resolved_location\":{"
            + "\"name\":\"" + escapeJson(coordinates.name()) + "\","
            + "\"country\":\"" + escapeJson(coordinates.country()) + "\","
            + "\"state\":" + (coordinates.state() == null ? "null" : "\"" + escapeJson(coordinates.state()) + "\"") + ","
            + "\"lat\":" + coordinates.lat() + ","
            + "\"lon\":" + coordinates.lon() + ","
            + "\"query\":\"" + escapeJson(query) + "\""
            + "}";
        if (json.endsWith("}")) {
            return json.substring(0, json.length() - 1) + "," + insertion + "}";
        }
        return json;
    }

    private static String callJson(String path, Map<String, String> queryParams) {
        if (API_KEY.isBlank()) {
            throw new IllegalStateException("Missing OPENWEATHER_API_KEY");
        }
        StringBuilder url = new StringBuilder(BASE_URL).append(path).append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                url.append("&");
            }
            first = false;
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        if (!first) {
            url.append("&");
        }
        url.append("appid=").append(URLEncoder.encode(API_KEY, StandardCharsets.UTF_8));

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url.toString())).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("OpenWeatherMap responded with HTTP " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenWeatherMap unavailable at " + BASE_URL, exception);
        }
    }

    private static String normalizeUnits(String units) {
        if ("standard".equals(units) || "metric".equals(units) || "imperial".equals(units)) {
            return units;
        }
        return "metric";
    }

    private static Map<String, String> mapOf(String... values) {
        Map<String, String> payload = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            payload.put(values[index], values[index + 1]);
        }
        return payload;
    }

    private static Map<String, Object> resource(String uri, String name, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", uri);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("mimeType", "text/plain");
        return payload;
    }

    private static Map<String, Object> prompt(String name, String description, List<Map<String, Object>> arguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("arguments", arguments);
        return payload;
    }

    private static Map<String, Object> argument(String name, String description, boolean required) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("required", required);
        return payload;
    }

    private static Map<String, Object> message(String role, String text) {
        return Map.of("role", role, "content", Map.of("type", "text", "text", text));
    }

    private static String extractJsonString(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = startQuote + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"' && json.charAt(index - 1) != '\\') {
                return builder.toString().replace("\\\"", "\"").replace("\\\\", "\\");
            }
            builder.append(current);
        }
        return null;
    }

    private static String extractJsonNumber(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && "-0123456789.".indexOf(json.charAt(end)) >= 0) {
            end++;
        }
        return start == end ? null : json.substring(start, end);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Coordinates(String lat, String lon, String name, String country, String state) {
    }
}
