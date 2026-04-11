# MCP Hello Java

Servidor MCP mínimo en Java usando solo biblioteca estándar.

Este directorio sirve como material de estudio para entender la mecánica de MCP sin SDK: framing por `Content-Length`, transporte por `stdio` y respuestas JSON-RPC.

En su estado actual, este MCP es un wrapper del backend REST Java. La herramienta `say_hello` no construye el saludo localmente: hace una llamada HTTP real a `GET /hello`.

## Objetivo

Exponer herramientas MCP sobre el backend REST Java:

- `name` opcional
- `lang` opcional
- `ip` opcional

La salida replica la idea del ejemplo REST, pero delegando realmente en el backend.

## Archivos

- `src/HelloMcpServer.java`: servidor MCP
- `src/HelloService.java`: utilidades locales para idiomas soportados

## Diseño del ejemplo

### Transporte

El servidor usa:

- `System.in` para leer requests
- `System.out` para escribir responses

Cada mensaje sigue este framing:

```text
Content-Length: <n>

<json>
```

Esto obliga a:

1. leer cabeceras
2. detectar `Content-Length`
3. leer exactamente `n` bytes
4. parsear el JSON recibido

### Métodos MCP implementados

El ejemplo implementa el conjunto mínimo útil:

- `initialize`
- `notifications/initialized`
- `tools/list`
- `tools/call`

### Herramienta expuesta

Las herramientas publicadas son:

- `say_hello`
- `get_hello_languages`

Su `inputSchema` declara:

- `name`
- `lang`
- `ip`

## Flujo de trabajo

### `initialize`

El cliente anuncia el inicio de la sesión.

El servidor responde con:

- versión de protocolo
- capacidades
- información del servidor

### `tools/list`

El cliente pregunta qué herramientas hay disponibles.

El servidor devuelve `say_hello` con su descripción y esquema de entrada.

### `tools/call`

El cliente ejecuta la herramienta.

El servidor:

1. extrae argumentos
2. llama al backend REST Java en `GET /hello`
3. devuelve el resultado como:

- `content`
- `structuredContent`
- `isError`

## Ejecutar

Desde la raíz del repositorio:

```bash
just run-java-mcp-hello
```

O manualmente:

```bash
make build-java-mcp-hello
java -cp mcp/hello/java/build HelloMcpServer
```

Por defecto, el wrapper apunta a:

```text
http://127.0.0.1:8081
```

Puedes cambiarlo con:

```bash
HELLO_API_BASE_URL=http://127.0.0.1:8081 java -cp mcp/hello/java/build HelloMcpServer
```

## Qué estudiar aquí

Este ejemplo es útil para estudiar:

- cómo funciona MCP sin librerías externas
- cómo envolver un backend REST con un servidor MCP
- cómo separar protocolo MCP de la llamada HTTP real
- cómo reflejar la misma idea funcional en Java y Python

## Limitaciones deliberadas

Para mantener el ejemplo corto:

- el parseo JSON sigue siendo manual y muy acotado
- la llamada al backend usa un flujo simple sin reintentos
- el servidor apunta a fines pedagógicos, no a producción

## Siguiente mejora natural

Una mejora razonable sería añadir mejor manejo de errores del backend REST y un contrato explícito de errores hacia el cliente MCP.
