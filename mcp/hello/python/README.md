# MCP Hello Python

Servidor MCP mínimo en Python usando solo biblioteca estándar.

Este directorio está pensado como material de estudio. La idea es mostrar cómo implementar un servidor MCP sin SDK, usando `stdio`, cabeceras `Content-Length` y mensajes JSON-RPC.

## Objetivo

Exponer una herramienta MCP llamada `say_hello` que replique la idea del backend REST:

- `name` opcional
- `lang` opcional
- respuesta JSON con `message`, `timestamp`, `ip`

## Archivos

- `server.py`: implementación del servidor MCP
- `hello_service.py`: lógica del saludo

## Qué hace `server.py`

`server.py` implementa manualmente las piezas mínimas de MCP:

1. Lee mensajes desde `stdin`.
2. Interpreta la cabecera `Content-Length`.
3. Parsea el cuerpo JSON.
4. Responde usando JSON-RPC 2.0.
5. Expone `initialize`, `tools/list` y `tools/call`.

## Flujo MCP implementado

### 1. `initialize`

El cliente MCP abre el proceso y envía `initialize`.

El servidor responde con:

- `protocolVersion`
- `capabilities`
- `serverInfo`

### 2. `notifications/initialized`

Después de inicializar, el cliente suele enviar esta notificación.

Como es una notificación, el servidor no responde.

### 3. `tools/list`

El cliente pregunta qué herramientas están disponibles.

El servidor devuelve una lista con una sola herramienta:

- `say_hello`

La herramienta declara su `inputSchema` JSON para que el cliente sepa qué argumentos acepta.

### 4. `tools/call`

El cliente llama a la herramienta con argumentos como:

```json
{
  "name": "say_hello",
  "arguments": {
    "name": "Raúl",
    "lang": "es",
    "ip": "203.0.113.10"
  }
}
```

El servidor:

1. valida el nombre de la herramienta
2. toma los argumentos
3. llama a `build_hello_payload`
4. responde con:

- `content`: texto serializado
- `structuredContent`: objeto JSON útil para clientes MCP
- `isError`: `false`

## Ejecutar

Desde la raíz del repositorio:

```bash
just run-python-mcp-hello
```

O directamente:

```bash
python3 mcp/hello/python/server.py
```

## Cómo probar manualmente

Como MCP usa `stdio`, una prueba real normalmente la hace un cliente MCP. Aun así, este código sirve para estudiar:

- framing con `Content-Length`
- ciclo request/response
- negociación inicial de capacidades
- publicación y ejecución de herramientas

## Puntos de estudio recomendados

### Framing

MCP sobre `stdio` no envía líneas JSON sueltas. Cada mensaje lleva:

```text
Content-Length: <n>

<json>
```

Por eso `server.py` primero lee cabeceras y luego exactamente `n` bytes del cuerpo.

### JSON-RPC

La forma del mensaje sigue JSON-RPC 2.0:

- `jsonrpc`
- `id`
- `method`
- `params`

Las respuestas devuelven:

- `result` en caso exitoso
- `error` si algo falla

### Tool schema

La herramienta declara un `inputSchema` en formato JSON Schema. Esto ayuda al cliente MCP a:

- mostrar formularios
- validar argumentos
- documentar automáticamente la herramienta

### Lógica separada

La lógica real del saludo vive en `hello_service.py`. Esa separación permite:

- reutilizar la idea en REST
- cambiar el transporte sin tocar la lógica principal
- estudiar mejor qué parte pertenece al protocolo y cuál al dominio

## Siguiente mejora natural

Una mejora razonable para este ejemplo sería extraer una librería compartida de saludo y hacer que REST y MCP la reutilicen sin duplicación.
