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
import dev.rafex.etherbrain.ports.runtime.ExecutionContext;
import dev.rafex.etherbrain.ports.session.ConversationState;
import dev.rafex.etherbrain.ports.tools.ToolRegistry;
import dev.rafex.etherbrain.tools.local.CompositeToolRegistry;
import dev.rafex.etherbrain.tools.local.InMemoryToolRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class AgentExampleApp {
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_PROMPT_TEMPLATE = "hello-es";
    private static final String DEFAULT_SESSION_ID = "agent-example-ether-brain";

    private AgentExampleApp() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = Path.of("").toAbsolutePath().normalize();
        try (HelloApiServerProcess ignored = HelloApiServerProcess.start(repoRoot);
             HelloMcpClient helloMcpClient = HelloMcpClient.start(repoRoot)) {
            if (args.length > 0 && "--check-mcp".equals(args[0])) {
                System.out.println(helloMcpClient.callHello("Ada Lovelace", "es", "127.0.0.1"));
                return;
            }

            if (args.length > 0 && "--check-mcp-languages".equals(args[0])) {
                System.out.println(helloMcpClient.getHelloLanguages());
                return;
            }
            if (args.length > 0 && "--check-mcp-languages".equals(args[0])) {
                System.out.println(helloMcpClient.getHelloLanguages());
                return;
            }

            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("DEEPSEEK_API_KEY es requerido para ejecutar el agente.");
            }

            String modelName = readEnvOrDefault("DEEPSEEK_MODEL", DEFAULT_MODEL);
            AgentConfig agentConfig = AgentConfig.defaults(Set.of("hello_mcp", "hello_mcp_languages"));
            ToolRegistry mcpToolRegistry = new McpHelloToolRegistry(helloMcpClient);
            ToolRegistry toolRegistry = new CompositeToolRegistry(List.of(
                mcpToolRegistry,
                new InMemoryToolRegistry()
            ));

            String sessionId = readEnvOrDefault("AGENT_SESSION_ID", DEFAULT_SESSION_ID);
            String prompt = args.length == 0
                ? resolvePrompt(readEnvOrDefault("AGENT_PROMPT_TEMPLATE", DEFAULT_PROMPT_TEMPLATE), agentConfig, sessionId)
                : String.join(" ", args);

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
                agentConfig
            );

            String result = runtime.run(sessionId, prompt);
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

    private static String resolvePrompt(String templateName, AgentConfig agentConfig, String sessionId) throws Exception {
        AgentExamplePromptRegistry promptRegistry = new AgentExamplePromptRegistry();
        ExecutionContext context = new ExecutionContext(sessionId, new ConversationState(), agentConfig);
        String promptOverride = System.getenv("PROMPT");
        if (promptOverride != null && !promptOverride.isBlank()) {
            return promptOverride;
        }
        return promptRegistry.get(templateName, context).content();
    }
}
