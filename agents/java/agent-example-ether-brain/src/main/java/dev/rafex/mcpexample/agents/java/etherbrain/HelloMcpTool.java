package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.etherbrain.ports.runtime.ExecutionContext;
import dev.rafex.etherbrain.ports.tools.Tool;
import dev.rafex.etherbrain.ports.tools.ToolResult;

public final class HelloMcpTool implements Tool {
    private final HelloMcpClient client;

    public HelloMcpTool(HelloMcpClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "hello_mcp";
    }

    @Override
    public String description() {
        return "Invoca el MCP hello local y devuelve un saludo estructurado.";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string", "description": "Nombre opcional a saludar." },
                    "lang": { "type": "string", "description": "Idioma opcional, por ejemplo en o es." },
                    "ip": { "type": "string", "description": "IP opcional a reflejar." }
                  },
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public ToolResult execute(String arguments, ExecutionContext context) throws Exception {
        String name = extractJsonString(arguments, "name");
        String lang = extractJsonString(arguments, "lang");
        String ip = extractJsonString(arguments, "ip");
        String payload = client.callHello(name, lang, ip);
        return new ToolResult(name(), true, payload);
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex < 0) {
            return null;
        }

        int colon = json.indexOf(':', keyIndex + pattern.length());
        if (colon < 0) {
            return null;
        }

        int valueStart = colon + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = valueStart + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '"' && json.charAt(i - 1) != '\\') {
                return builder.toString()
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");
            }
            builder.append(current);
        }
        return null;
    }
}
