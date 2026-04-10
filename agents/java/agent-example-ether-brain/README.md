# Agent Example EtherBrain

Ejemplo de agente Java que conecta tres piezas:

- `ether-brain` como runtime de agente
- `ether-ai-deepseek` como adaptador de modelo
- `mcp/hello/java` como tool MCP local

La idea es mostrar una evolucion del ejemplo `hello`: el servidor MCP sigue siendo pequeno y didactico, pero ahora un agente lo usa como herramienta dentro de un loop de decision.

## Estructura

- `src/main/java/dev/rafex/mcpexample/agents/java/etherbrain/AgentExampleApp.java`: entrypoint CLI
- `src/main/java/dev/rafex/mcpexample/agents/java/etherbrain/DeepSeekEtherBrainModelClient.java`: adaptador entre EtherBrain y DeepSeek
- `src/main/java/dev/rafex/mcpexample/agents/java/etherbrain/HelloMcpTool.java`: tool EtherBrain que llama al MCP `hello`
- `src/main/java/dev/rafex/mcpexample/agents/java/etherbrain/HelloMcpClient.java`: cliente MCP minimo sobre `stdio`

## Requisitos

- Java 21
- Maven 3.9+
- `DEEPSEEK_API_KEY` definido para ejecutar el agente completo

## Compilar

Desde la raiz del repositorio:

```bash
just build-java-agent-example-ether-brain
```

Ese comando tambien recompila antes el MCP Java `hello`, porque el agente lo lanza como proceso hijo.

## Verificar solo la tool MCP

Sin usar DeepSeek, puedes probar la integracion MCP local:

```bash
make run-java-agent-example-ether-brain-check-mcp
```

El comando arranca `mcp/hello/java`, ejecuta `say_hello` y muestra el JSON estructurado devuelto por el servidor.

## Ejecutar el agente

Define la llave y ejecuta:

```bash
export DEEPSEEK_API_KEY="tu_api_key"
make run-java-agent-example-ether-brain PROMPT="Usa la tool hello_mcp para saludar a Ada en es y luego responde breve."
```

Tambien puedes usar el `justfile`:

```bash
DEEPSEEK_API_KEY="tu_api_key" just run-java-agent-example-ether-brain
```

## Variables soportadas

- `DEEPSEEK_API_KEY`: requerida para modo agente
- `DEEPSEEK_MODEL`: opcional, por defecto `deepseek-chat`
- `PROMPT`: prompt a enviar al runtime, por defecto pide un saludo en espanol

## Como funciona

1. El proceso principal arranca el MCP `hello` Java local por `stdio`.
2. Se registra una tool EtherBrain llamada `hello_mcp`.
3. El adaptador `DeepSeekEtherBrainModelClient` convierte los mensajes de EtherBrain en mensajes de Ether AI.
4. DeepSeek responde siguiendo el protocolo textual de EtherBrain:
   `TOOL:<tool>`
   `ARGS:<json>`
   o `FINAL:<texto>`
5. Si el modelo pide `hello_mcp`, el runtime ejecuta la tool y reinyecta el resultado en la conversacion.

## Por que este ejemplo existe

Este ejemplo documenta explicitamente la excepcion del repositorio para usar Ether cuando aporta valor didactico. Aqui el valor esta en mostrar:

- un runtime de agente pequeno pero reutilizable
- un adaptador de modelo real
- una tool MCP local consumida desde un agente
- la transicion desde ejemplos base con biblioteca estandar hacia una integracion mas completa
