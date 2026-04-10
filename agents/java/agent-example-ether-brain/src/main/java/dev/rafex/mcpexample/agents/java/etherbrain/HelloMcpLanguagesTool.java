package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.etherbrain.ports.runtime.ExecutionContext;
import dev.rafex.etherbrain.ports.tools.Tool;
import dev.rafex.etherbrain.ports.tools.ToolResult;

public final class HelloMcpLanguagesTool implements Tool {
    private final HelloMcpClient client;

    public HelloMcpLanguagesTool(HelloMcpClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "hello_mcp_languages";
    }

    @Override
    public String description() {
        return "Consulta el MCP hello local y devuelve la cantidad de idiomas soportados y sus códigos.";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {},
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public ToolResult execute(String arguments, ExecutionContext context) throws Exception {
        return new ToolResult(name(), true, client.getHelloLanguages());
    }
}
