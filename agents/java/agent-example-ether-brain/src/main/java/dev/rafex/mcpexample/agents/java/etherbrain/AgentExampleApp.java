package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.ether.ai.deepseek.chat.DeepSeekChatModel;
import dev.rafex.ether.ai.deepseek.config.DeepSeekConfig;
import dev.rafex.etherbrain.core.policy.DefaultPolicyEngine;
import dev.rafex.etherbrain.core.prompt.PromptBuilder;
import dev.rafex.etherbrain.core.runtime.AgentLoop;
import dev.rafex.etherbrain.core.runtime.AgentRuntime;
import dev.rafex.etherbrain.core.tools.DefaultToolExecutor;
import dev.rafex.etherbrain.infra.memory.InMemorySessionStore;
import dev.rafex.etherbrain.ports.runtime.AgentConfig;
import dev.rafex.etherbrain.tools.local.InMemoryToolRegistry;
import java.nio.file.Path;
import java.util.Set;

public final class AgentExampleApp {
    private static final String DEFAULT_PROMPT = "Usa la tool hello_mcp para saludar a Ada Lovelace en es y responde breve.";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    private AgentExampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = Path.of("").toAbsolutePath().normalize();
        try (HelloMcpClient helloMcpClient = HelloMcpClient.start(repoRoot)) {
            if (args.length > 0 && "--check-mcp".equals(args[0])) {
                System.out.println(helloMcpClient.callHello("Ada Lovelace", "es", "127.0.0.1"));
                return;
            }

            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("DEEPSEEK_API_KEY es requerido para ejecutar el agente.");
            }

            String modelName = readEnvOrDefault("DEEPSEEK_MODEL", DEFAULT_MODEL);
            String prompt = args.length == 0 ? readEnvOrDefault("PROMPT", DEFAULT_PROMPT) : String.join(" ", args);

            InMemoryToolRegistry toolRegistry = new InMemoryToolRegistry()
                .register(new HelloMcpTool(helloMcpClient))
                .register(new HelloMcpLanguagesTool(helloMcpClient));

            DeepSeekChatModel chatModel = new DeepSeekChatModel(DeepSeekConfig.of(apiKey));
            AgentLoop agentLoop = new AgentLoop(
                new DeepSeekEtherBrainModelClient(chatModel, modelName),
                toolRegistry,
                new DefaultToolExecutor(toolRegistry),
                new PromptBuilder(),
                new DefaultPolicyEngine()
            );

            AgentRuntime runtime = new AgentRuntime(
                new InMemorySessionStore(),
                agentLoop,
                AgentConfig.defaults(Set.of("hello_mcp", "hello_mcp_languages"))
            );

            String result = runtime.run("agent-example-ether-brain", prompt);
            System.out.println(result);
        }
    }

    private static String readEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
