import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DateService {
    private static final Map<String, String[]> LOCATIONS = new LinkedHashMap<>();

    static {
        put("us", "United States", "America/New_York");
        put("ca", "Canada", "America/Toronto");
        put("br", "Brazil", "America/Sao_Paulo");
        put("ar", "Argentina", "America/Argentina/Buenos_Aires");
        put("cl", "Chile", "America/Santiago");
        put("co", "Colombia", "America/Bogota");
        put("pe", "Peru", "America/Lima");
        put("gb", "United Kingdom", "Europe/London");
        put("ie", "Ireland", "Europe/Dublin");
        put("fr", "France", "Europe/Paris");
        put("de", "Germany", "Europe/Berlin");
        put("es", "Spain", "Europe/Madrid");
        put("it", "Italy", "Europe/Rome");
        put("nl", "Netherlands", "Europe/Amsterdam");
        put("be", "Belgium", "Europe/Brussels");
        put("ch", "Switzerland", "Europe/Zurich");
        put("pt", "Portugal", "Europe/Lisbon");
        put("se", "Sweden", "Europe/Stockholm");
        put("no", "Norway", "Europe/Oslo");
        put("fi", "Finland", "Europe/Helsinki");
        put("pl", "Poland", "Europe/Warsaw");
        put("ua", "Ukraine", "Europe/Kyiv");
        put("tr", "Turkey", "Europe/Istanbul");
        put("ru", "Russia", "Europe/Moscow");
        put("sa", "Saudi Arabia", "Asia/Riyadh");
        put("ae", "United Arab Emirates", "Asia/Dubai");
        put("eg", "Egypt", "Africa/Cairo");
        put("za", "South Africa", "Africa/Johannesburg");
        put("ng", "Nigeria", "Africa/Lagos");
        put("ke", "Kenya", "Africa/Nairobi");
        put("in", "India", "Asia/Kolkata");
        put("pk", "Pakistan", "Asia/Karachi");
        put("bd", "Bangladesh", "Asia/Dhaka");
        put("cn", "China", "Asia/Shanghai");
        put("jp", "Japan", "Asia/Tokyo");
        put("kr", "South Korea", "Asia/Seoul");
        put("sg", "Singapore", "Asia/Singapore");
        put("id", "Indonesia", "Asia/Jakarta");
        put("th", "Thailand", "Asia/Bangkok");
        put("vn", "Vietnam", "Asia/Ho_Chi_Minh");
        put("ph", "Philippines", "Asia/Manila");
        put("my", "Malaysia", "Asia/Kuala_Lumpur");
        put("au", "Australia", "Australia/Sydney");
        put("nz", "New Zealand", "Pacific/Auckland");
        put("il", "Israel", "Asia/Jerusalem");
        put("ir", "Iran", "Asia/Tehran");
        put("ma", "Morocco", "Africa/Casablanca");
        put("dz", "Algeria", "Africa/Algiers");
        put("et", "Ethiopia", "Africa/Addis_Ababa");
        put("gh", "Ghana", "Africa/Accra");
        put("mx-central", "Mexico Central", "America/Mexico_City");
        put("mx-northwest", "Mexico Northwest", "America/Tijuana");
        put("mx-southeast", "Mexico Southeast", "America/Cancun");
    }

    private DateService() {
    }

    public static Map<String, Object> buildLocationsPayload() {
        List<Map<String, Object>> locations = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : LOCATIONS.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("location", entry.getKey());
            item.put("label", entry.getValue()[0]);
            item.put("timezone", entry.getValue()[1]);
            locations.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("location_count", locations.size());
        payload.put("locations", locations);
        return payload;
    }

    public static Map<String, Object> buildTimePayload(String location, String ip) {
        String normalized = normalizeLocation(location);
        String[] metadata = LOCATIONS.get(normalized);
        ZonedDateTime localNow = ZonedDateTime.now(ZoneId.of(metadata[1]));
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("location", normalized);
        payload.put("label", metadata[0]);
        payload.put("timezone", metadata[1]);
        payload.put("local_time", localNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        payload.put("utc_offset", localNow.format(DateTimeFormatter.ofPattern("XX")));
        payload.put("utc_time", utcNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC+00:00'")));
        payload.put("ip", ip);
        return payload;
    }

    public static Map<String, Object> buildResourcesPayload() {
        List<Map<String, Object>> resources = new ArrayList<>();
        resources.add(resource("date://auth-reference", "auth-reference", "Resumen de autenticación requerida por el servicio date."));
        resources.add(resource("date://location-reference", "location-reference", "Listado de ubicaciones soportadas y sus zonas horarias."));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resources", resources);
        return payload;
    }

    public static Map<String, Object> buildResourceContentsPayload(String resourceUri) {
        String text;
        if ("date://auth-reference".equals(resourceUri)) {
            text = String.join("\n",
                "Date API authentication reference",
                "- Header Authorization: Bearer <token>",
                "- Header X-Date-Client: <client-id>",
                "- Endpoints require authentication for GET requests"
            );
        } else if ("date://location-reference".equals(resourceUri)) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String[]> entry : LOCATIONS.entrySet()) {
                if (!first) {
                    builder.append('\n');
                }
                first = false;
                builder.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue()[0])
                    .append(" (")
                    .append(entry.getValue()[1])
                    .append(")");
            }
            text = builder.toString();
        } else {
            throw new IllegalArgumentException(resourceUri);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("uri", resourceUri);
        content.put("mimeType", "text/plain");
        content.put("text", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contents", List.of(content));
        return payload;
    }

    public static Map<String, Object> buildPromptsPayload() {
        List<Map<String, Object>> prompts = new ArrayList<>();
        prompts.add(prompt(
            "single-location-time",
            "Pide la hora actual de una ubicación concreta usando get_current_time.",
            List.of(argument("location", "Código de ubicación soportado.", true))
        ));
        prompts.add(prompt(
            "compare-locations",
            "Pide comparar la hora actual entre dos ubicaciones.",
            List.of(
                argument("from_location", "Primera ubicación.", true),
                argument("to_location", "Segunda ubicación.", true)
            )
        ));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prompts", prompts);
        return payload;
    }

    public static Map<String, Object> buildPromptDetailsPayload(String promptName, String fromLocation, String toLocation) {
        if ("single-location-time".equals(promptName)) {
            String location = normalizeLocation(fromLocation == null ? "mx-central" : fromLocation);
            return promptDetails(
                "Prompt para obtener la hora de una sola ubicación.",
                List.of(
                    message("system", "Usa la herramienta get_current_time y responde con la hora obtenida."),
                    message("user", "Dime la hora actual para " + location + " usando get_current_time.")
                )
            );
        }
        if ("compare-locations".equals(promptName)) {
            String first = normalizeLocation(fromLocation == null ? "mx-central" : fromLocation);
            String second = normalizeLocation(toLocation == null ? "us" : toLocation);
            return promptDetails(
                "Prompt para comparar dos ubicaciones usando get_current_time.",
                List.of(
                    message("system", "Usa get_current_time para ambas ubicaciones y compara los resultados."),
                    message("user", "Compara la hora actual entre " + first + " y " + second + " usando get_current_time para cada ubicación.")
                )
            );
        }
        throw new IllegalArgumentException(promptName);
    }

    public static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "mx-central";
        }
        String normalized = location.trim().toLowerCase();
        if (!LOCATIONS.containsKey(normalized)) {
            throw new IllegalArgumentException(location);
        }
        return normalized;
    }

    private static void put(String key, String label, String zoneId) {
        LOCATIONS.put(key, new String[] {label, zoneId});
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

    private static Map<String, Object> promptDetails(String description, List<Map<String, Object>> messages) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("description", description);
        payload.put("messages", messages);
        return payload;
    }

    private static Map<String, Object> message(String role, String text) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "text");
        content.put("text", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", role);
        payload.put("content", content);
        return payload;
    }
}
