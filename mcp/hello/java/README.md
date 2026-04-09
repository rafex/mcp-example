# MCP Hello Java

Servidor MCP mínimo en Java usando solo biblioteca estándar.

Este directorio sirve como material de estudio para entender la mecánica de MCP sin SDK: framing por `Content-Length`, transporte por `stdio` y respuestas JSON-RPC.

## Objetivo

Exponer una herramienta MCP llamada `say_hello` con:

- `name` opcional
- `lang` opcional
- `ip` opcional

La salida replica la idea del ejemplo REST.

## Archivos

- `src/HelloMcpServer.java`: servidor MCP
- `src/HelloService.java`: lógica del saludo

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

La herramienta publicada es:

- `say_hello`

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
2. invoca `HelloService.buildHelloPayload`
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

## Qué estudiar aquí

Este ejemplo es útil para estudiar:

- cómo funciona MCP sin librerías externas
- cómo separar protocolo y lógica de dominio
- cómo modelar una herramienta pequeña pero completa
- cómo reflejar la misma idea funcional en Java y Python

## Limitaciones deliberadas

Para mantener el ejemplo corto:

- el parseo JSON es manual y muy acotado
- solo se soporta la herramienta de saludo
- el servidor apunta a fines pedagógicos, no a producción

## Siguiente mejora natural

Una mejora razonable sería reemplazar el parseo JSON manual por una librería dedicada o por Ether si se quiere mostrar una evolución del ejemplo desde una base totalmente estándar.
