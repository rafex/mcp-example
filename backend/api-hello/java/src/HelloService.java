import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HelloService {
    private static final Map<String, String> GREETINGS = Map.of(
        "en", "Hello",
        "zh", "Ni hao",
        "hi", "Namaste",
        "es", "Hola",
        "fr", "Bonjour",
        "ar", "Marhaban",
        "bn", "Nomoskar",
        "pt", "Ola",
        "ru", "Privet",
        "ur", "Assalam o Alaikum"
    );

    private HelloService() {
    }

    public static Map<String, Object> buildLanguagesPayload() {
        List<String> languages = GREETINGS.keySet().stream().sorted().toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language_count", languages.size());
        payload.put("languages", languages);
        return payload;
    }

    public static Map<String, Object> buildResourcesPayload() {
        List<Map<String, Object>> resources = new ArrayList<>();
        resources.add(resourceDefinition(
            "hello://service-overview",
            "service-overview",
            "Resumen de los endpoints REST disponibles del ejemplo hello.",
            "text/plain"
        ));
        resources.add(resourceDefinition(
            "hello://language-reference",
            "language-reference",
            "Referencia de idiomas soportados y su saludo base.",
            "text/plain"
        ));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resources", resources);
        return payload;
    }

    public static Map<String, Object> buildResourceContentsPayload(String resourceUri) {
        String text;
        if ("hello://service-overview".equals(resourceUri)) {
            text = String.join("\n",
                "Hello API service overview",
                "- GET /hello",
                "- POST /hello",
                "- OPTIONS /hello",
                "- GET /hello/languages",
                "- GET /hello/resources",
                "- GET /hello/prompts"
            );
        } else if ("hello://language-reference".equals(resourceUri)) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String code : GREETINGS.keySet().stream().sorted().toList()) {
                if (!first) {
                    builder.append('\n');
                }
                first = false;
                builder.append("- ").append(code).append(": ").append(GREETINGS.get(code));
            }
            text = builder.toString();
        } else {
            throw new IllegalArgumentException("Unsupported resource URI: " + resourceUri);
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
        prompts.add(promptDefinition(
            "greet-user",
            "Genera instrucciones para saludar a una persona en un idioma concreto usando say_hello.",
            List.of(
                promptArgument("name", "Nombre de la persona a saludar.", true),
                promptArgument("lang", "Idioma deseado para el saludo.", true)
            )
        ));
        prompts.add(promptDefinition(
            "language-report",
            "Genera instrucciones para listar idiomas y luego saludar a alguien en todos ellos.",
            List.of(
                promptArgument("name", "Nombre que se usará en todos los saludos.", true)
            )
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prompts", prompts);
        return payload;
    }

    public static Map<String, Object> buildPromptDetailsPayload(String promptName, String name, String lang) {
        if ("greet-user".equals(promptName)) {
            String resolvedName = (name == null || name.isBlank()) ? "Ada Lovelace" : name;
            String resolvedLang = normalizeLang(lang);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("description", "Prompt para pedir un saludo único usando say_hello.");
            payload.put("messages", List.of(
                promptMessage("system", "Usa la herramienta say_hello y responde solo con el resultado obtenido."),
                promptMessage("user", "Saluda a " + resolvedName + " en " + resolvedLang + " usando say_hello.")
            ));
            return payload;
        }

        if ("language-report".equals(promptName)) {
            String resolvedName = (name == null || name.isBlank()) ? "Pedro" : name;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("description", "Prompt para pedir un reporte de idiomas y saludos usando herramientas MCP.");
            payload.put("messages", List.of(
                promptMessage("system", "Usa solo tools disponibles y no inventes idiomas."),
                promptMessage(
                    "user",
                    "Usa get_hello_languages para obtener los idiomas soportados y luego usa say_hello para saludar a "
                        + resolvedName + " en cada idioma."
                )
            ));
            return payload;
        }

        throw new IllegalArgumentException("Unsupported prompt: " + promptName);
    }

    public static Map<String, Object> buildHelloPayload(String name, String lang, String ip) {
        String normalizedLang = normalizeLang(lang);
        String greeting = GREETINGS.get(normalizedLang);
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss 'UTC+00:00'"));

        Map<String, Object> payload = new LinkedHashMap<>();
        if (name != null && !name.isBlank()) {
            payload.put("message", greeting + " " + name + "!");
            payload.put("timestamp", timestamp);
            payload.put("ip", ip);
            payload.put("lang", normalizedLang);
            payload.put("has_name", true);
            payload.put("name", name);
            return payload;
        }

        payload.put("message", greeting + "!");
        payload.put("timestamp", timestamp);
        payload.put("ip", ip);
        payload.put("lang", normalizedLang);
        payload.put("has_name", false);
        return payload;
    }

    public static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        String normalized = lang.trim().toLowerCase(Locale.ROOT);
        if (GREETINGS.containsKey(normalized)) {
            return normalized;
        }
        return "en";
    }

    private static Map<String, Object> resourceDefinition(String uri, String name, String description, String mimeType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", uri);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("mimeType", mimeType);
        return payload;
    }

    private static Map<String, Object> promptDefinition(String name, String description, List<Map<String, Object>> arguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("arguments", arguments);
        return payload;
    }

    private static Map<String, Object> promptArgument(String name, String description, boolean required) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("required", required);
        return payload;
    }

    private static Map<String, Object> promptMessage(String role, String text) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "text");
        content.put("text", text);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
