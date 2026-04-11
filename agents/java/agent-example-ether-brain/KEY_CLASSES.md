# Clases Clave

## Runtime y composicion

- `AgentExampleApp`:
  punto de entrada. Construye `AgentConfig`, resuelve el prompt inicial, arranca el cliente MCP y crea el `AgentRuntime`.
- `McpHelloToolRegistry`:
  encapsula las tools MCP del ejemplo y evita registrar tools sueltas desde `main`.
- `CompositeToolRegistry`:
  viene de `ether-brain-tools-local` y permite combinar el registro MCP con otros registros futuros sin cambiar el loop del agente.

## Prompts

- `AgentExamplePromptRegistry`:
  implementa `PromptRegistry` y muestra la nueva API de prompts de `ether-brain`.
  Define descriptores legibles y genera `PromptTemplate` concretos.
- `PromptDescriptor`:
  metadato navegable de cada prompt disponible.
- `PromptTemplate`:
  prompt final que se entrega al runtime.

## Modelo

- `DeepSeekEtherBrainModelClient`:
  adaptador entre `ether-brain` y `ether-ai-deepseek`.
  Convierte mensajes de EtherBrain en `AiMessage`, invoca DeepSeek y traduce la salida a `FinalAnswer` o `ToolRequest`.

## MCP

- `HelloMcpClient`:
  cliente MCP minimo sobre `stdio`.
  Hace `initialize`, envia `tools/call` y extrae `structuredContent` de la respuesta.
- `HelloMcpTool`:
  expone `say_hello` como tool de EtherBrain.
- `HelloMcpLanguagesTool`:
  expone `get_hello_languages` como tool de EtherBrain.

## Flujo resumido

1. `AgentExampleApp` arranca `HelloMcpClient`.
2. `AgentExampleApp` crea `McpHelloToolRegistry` y lo compone con `CompositeToolRegistry`.
3. `AgentExamplePromptRegistry` resuelve el prompt inicial.
4. `AgentRuntime` ejecuta el loop.
5. `DeepSeekEtherBrainModelClient` decide entre respuesta final o llamada a tool.
6. Si pide tool, EtherBrain ejecuta `hello_mcp` o `hello_mcp_languages` contra el MCP local.
