package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.etherbrain.ports.prompts.PromptDescriptor;
import dev.rafex.etherbrain.ports.prompts.PromptRegistry;
import dev.rafex.etherbrain.ports.prompts.PromptTemplate;
import dev.rafex.etherbrain.ports.runtime.ExecutionContext;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AgentExamplePromptRegistry implements PromptRegistry {
    private static final Map<String, PromptDescriptor> DESCRIPTORS = createDescriptors();

    @Override
    public Optional<PromptDescriptor> find(String name) {
        return Optional.ofNullable(DESCRIPTORS.get(name));
    }

    @Override
    public Collection<PromptDescriptor> all() {
        return DESCRIPTORS.values();
    }

    @Override
    public PromptTemplate get(String name, ExecutionContext context) {
        return switch (name) {
            case "hello-es" -> new PromptTemplate(
                name,
                """
                Usa la tool hello_mcp para saludar a Ada Lovelace en es.
                Responde breve y menciona que la respuesta vino del MCP hello local.
                """
            );
            case "languages-then-hello" -> new PromptTemplate(
                name,
                """
                Primero usa hello_mcp_languages para revisar los idiomas soportados.
                Luego usa hello_mcp para saludar a Ada Lovelace en es.
                Responde breve e incluye el codigo de idioma usado.
                """
            );
            case "session-aware" -> new PromptTemplate(
                name,
                """
                Usa hello_mcp_languages y luego hello_mcp.
                La sesion actual es %s.
                Las tools habilitadas son %s.
                Responde breve.
                """.formatted(context.sessionId(), context.agentConfig().enabledTools())
            );
            default -> throw new IllegalArgumentException("Prompt no soportado: " + name);
        };
    }

    private static Map<String, PromptDescriptor> createDescriptors() {
        Map<String, PromptDescriptor> descriptors = new LinkedHashMap<>();
        descriptors.put("hello-es", new PromptDescriptor(
            "hello-es",
            "Saluda en espanol usando la tool MCP hello."
        ));
        descriptors.put("languages-then-hello", new PromptDescriptor(
            "languages-then-hello",
            "Consulta idiomas soportados y luego ejecuta el saludo."
        ));
        descriptors.put("session-aware", new PromptDescriptor(
            "session-aware",
            "Ejemplo que incluye datos del ExecutionContext al generar el prompt."
        ));
        return Map.copyOf(descriptors);
    }
}
