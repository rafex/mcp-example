package dev.rafex.mcpexample.agents.java.etherbrain;

import dev.rafex.etherbrain.ports.tools.Tool;
import dev.rafex.etherbrain.ports.tools.ToolRegistry;
import dev.rafex.etherbrain.tools.local.InMemoryToolRegistry;
import java.util.Collection;
import java.util.Optional;

public final class McpHelloToolRegistry implements ToolRegistry {
    private final InMemoryToolRegistry delegate;

    public McpHelloToolRegistry(HelloMcpClient client) {
        this.delegate = new InMemoryToolRegistry()
            .register(new HelloMcpTool(client))
            .register(new HelloMcpLanguagesTool(client));
    }

    @Override
    public Optional<Tool> find(String name) {
        return delegate.find(name);
    }

    @Override
    public Collection<Tool> all() {
        return delegate.all();
    }
}
