# Uso Del Agente

## Requisitos

- Java 21
- Maven 3.9+
- `DEEPSEEK_API_KEY` para modo agente

## Compilar

Desde la raiz del repositorio:

```bash
just build-java-agent-example-ether-brain
```

Ese build recompila primero `mcp/hello/java`, porque el agente lo ejecuta como proceso hijo.

## Verificaciones locales

Probar el saludo MCP sin modelo:

```bash
just run-java-agent-example-ether-brain-check-mcp
```

Probar la consulta de idiomas soportados por el MCP:

```bash
just run-java-agent-example-ether-brain-check-mcp-languages
```

## Ejecutar el agente

Con el template por defecto:

```bash
DEEPSEEK_API_KEY="tu_api_key" just run-java-agent-example-ether-brain
```

Con un prompt directo:

```bash
DEEPSEEK_API_KEY="tu_api_key" \
PROMPT="Usa hello_mcp_languages y luego hello_mcp para saludar a Ada en es. Responde breve." \
just run-java-agent-example-ether-brain
```

Con un template del `PromptRegistry`:

```bash
DEEPSEEK_API_KEY="tu_api_key" \
AGENT_PROMPT_TEMPLATE="languages-then-hello" \
just run-java-agent-example-ether-brain
```

## Variables de entorno

- `DEEPSEEK_API_KEY`: requerida para llamadas al modelo
- `DEEPSEEK_MODEL`: modelo DeepSeek, por defecto `deepseek-chat`
- `PROMPT`: prompt libre; tiene prioridad sobre cualquier template
- `AGENT_PROMPT_TEMPLATE`: nombre del prompt template; por defecto `hello-es`
- `AGENT_SESSION_ID`: id de sesion de EtherBrain; por defecto `agent-example-ether-brain`

## Templates disponibles

- `hello-es`: saludo simple en espanol usando `hello_mcp`
- `languages-then-hello`: primero revisa idiomas y luego saluda
- `session-aware`: ejemplo que incorpora datos del `ExecutionContext`
