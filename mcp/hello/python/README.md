# MCP Hello Python

Servidor MCP mínimo en Python usando solo biblioteca estándar.

Este directorio está pensado como material de estudio. La idea es mostrar cómo implementar un servidor MCP sin SDK, usando `stdio`, cabeceras `Content-Length` y mensajes JSON-RPC.

En su estado actual, este MCP es un wrapper del backend REST Python. La herramienta `say_hello` no calcula el saludo localmente: hace una llamada HTTP real a `GET /hello`.

## Objetivo

Exponer herramientas MCP sobre el backend REST:

- `name` opcional
- `lang` opcional
- respuesta JSON con `message`, `timestamp`, `ip`

## Archivos

- `server.py`: implementación del servidor MCP
- `hello_service.py`: utilidades locales para idiomas soportados

## Qué hace `server.py`

`server.py` implementa manualmente las piezas mínimas de MCP:

1. Lee mensajes desde `stdin`.
2. Interpreta la cabecera `Content-Length`.
3. Parsea el cuerpo JSON.
4. Responde usando JSON-RPC 2.0.
5. Expone `initialize`, `tools/list` y `tools/call`.
6. Para `say_hello`, consume el backend REST Python por HTTP.

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

El servidor devuelve dos herramientas:

- `say_hello`
- `get_hello_languages`

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
3. llama al backend REST en `GET /hello`
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

Por defecto, el wrapper apunta a:

```text
http://127.0.0.1:8080
```

Puedes cambiarlo con:

```bash
HELLO_API_BASE_URL=http://127.0.0.1:8080 python3 mcp/hello/python/server.py
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

### Wrapper sobre REST

En esta variante, MCP y REST ya no son dos implementaciones paralelas del saludo.

Ahora:

- el backend REST resuelve el caso de uso
- el MCP delega en el backend
- el MCP actúa como adaptador para clientes compatibles con herramientas MCP

## Siguiente mejora natural

Una mejora razonable para este ejemplo sería añadir reintentos, timeouts configurables y una estrategia más explícita para propagar errores del backend REST al cliente MCP.
