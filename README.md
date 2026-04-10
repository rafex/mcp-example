# mcp-example

Proyecto de estudio para construir, comparar y entender:

1. APIs REST simples.
2. Servidores MCP paso a paso.
3. Implementaciones paralelas en Python y Java.

El repositorio está orientado a aprendizaje práctico: código pequeño, sin frameworks de aplicación, usando principalmente bibliotecas estándar. La única excepción permitida es [Ether](https://ether.rafex.io/) cuando aporte valor directo.

## Primer ejemplo

El primer caso de estudio del repositorio es `hello`.

Incluye:

- un backend REST en Python
- un backend REST en Java
- un servidor MCP en Python
- un servidor MCP en Java
- un agente Java que usa el MCP `hello` a traves de EtherBrain y DeepSeek

Todos implementan la misma idea base:

- endpoint o herramienta `hello`
- parámetro opcional `name`
- parámetro opcional `lang`
- respuesta JSON con saludo, hora UTC e IP

## Comportamiento de `hello`

Ruta REST:

- `GET /hello`

Parámetros soportados:

- `name`: nombre opcional de la persona a saludar
- `lang`: idioma del saludo, por ejemplo `en` o `es`

La respuesta cambia si se envía `name` o no.

Ejemplo sin nombre:

```json
{
  "message": "Hello!",
  "timestamp": "23:00:00 UTC+00:00",
  "ip": "127.0.0.1",
  "lang": "en",
  "has_name": false
}
```

Ejemplo con nombre:

```json
{
  "message": "Hello Raúl!",
  "timestamp": "23:00:00 UTC+00:00",
  "ip": "127.0.0.1",
  "lang": "en",
  "has_name": true,
  "name": "Raúl"
}
```

## Idiomas soportados

El ejemplo soporta 10 idiomas de uso muy extendido:

- `en`: English
- `zh`: Chinese
- `hi`: Hindi
- `es`: Spanish
- `fr`: French
- `ar`: Arabic
- `bn`: Bengali
- `pt`: Portuguese
- `ru`: Russian
- `ur`: Urdu

Si `lang` no se envía, el valor por defecto es `en`.

## Estructura actual

```text
.
├── AGENTS.md
├── README.md
├── Makefile
├── justfile
├── container/
│   └── hello/
│       ├── README.md
│       ├── Dockerfile.python-api
│       ├── Dockerfile.java-api
│       └── compose.yaml
├── openapi/
│   ├── README.md
│   ├── api-hello.yaml
│   └── mcp-hello.http.json
├── backend/
│   └── api-hello/
│       ├── java/
│       │   ├── README.md
│       │   └── src/
│       └── python/
│           ├── README.md
│           ├── hello_service.py
│           └── server.py
├── agents/
│   └── java/
│       └── agent-example-ether-brain/
│           ├── README.md
│           ├── pom.xml
│           └── src/
└── mcp/
    ├── docs/
    │   ├── README.md
    │   ├── what-is-mcp-es.md
    │   ├── what-is-mcp-en.md
    │   ├── stdio-vs-network-es.md
    │   ├── stdio-vs-network-en.md
    │   ├── mcp-server-es.md
    │   └── mcp-server-en.md
    └── hello/
        ├── java/
        │   ├── README.md
        │   └── src/
        └── python/
            ├── README.md
            ├── hello_service.py
            └── server.py
```

## Tareas

Convención del proyecto:

- `just` es la interfaz principal de trabajo diario
- `make` concentra tareas de build y ejecución
- `just` puede invocar `make`
- `make` no debe invocar `just`

## Comandos rápidos

Ejecutar API REST en Python:

```bash
just run-python-api-hello
```

Ejecutar API REST en Java:

```bash
just run-java-api-hello
```

Construir imagen Docker del backend Python:

```bash
just docker-build-python-api-hello
```

Construir imagen Docker del backend Java:

```bash
just docker-build-java-api-hello
```

Levantar ambos backends con contenedores:

```bash
just docker-up-hello
```

Ejecutar MCP en Python:

```bash
just run-python-mcp-hello
```

Ejecutar MCP en Java:

```bash
just run-java-mcp-hello
```

Compilar el agente Java con EtherBrain:

```bash
just build-java-agent-example-ether-brain
```

Verificar la tool MCP del agente sin usar DeepSeek:

```bash
just run-java-agent-example-ether-brain-check-mcp
```

Ejecutar el agente Java con DeepSeek:

```bash
DEEPSEEK_API_KEY="tu_api_key" just run-java-agent-example-ether-brain
```

## Propósito pedagógico

Este repositorio busca responder estas preguntas:

1. Cómo levantar una API HTTP mínima sin frameworks.
2. Cómo parsear parámetros y devolver JSON con herramientas nativas.
3. Cómo implementar MCP sobre `stdio` y JSON-RPC sin depender de SDKs.
4. Cómo mantener una idea funcional equivalente entre Python y Java.
5. Cómo conectar un runtime de agente con una tool MCP local usando Ether.
6. Cómo documentar el proceso para que sirva como material de estudio.

## OpenAPI y clientes API

La carpeta `openapi/` concentra las definiciones consumibles por herramientas como Bruno o Postman.

- Los endpoints REST se describen con OpenAPI.
- Los ejemplos MCP se documentan con mensajes JSON-RPC de referencia, porque MCP sobre `stdio` no encaja de forma natural en OpenAPI.
- Cuando un MCP tenga una variante HTTP o SSE en el futuro, esa interfaz sí podrá describirse también con OpenAPI.

## Documentación MCP

La carpeta `mcp/docs/` contiene documentación conceptual para estudio.

Ahí se explica:

- qué es MCP
- cómo funciona MCP sobre `stdio`
- la diferencia entre `stdio` y transportes de red
- qué tipos de transporte puede usar un servidor MCP

## Próximos pasos

1. Extender `hello` con más validaciones y pruebas.
2. Añadir más ejemplos REST/MCP sobre otros casos simples.
3. Comparar una implementación totalmente estándar con una variante usando Ether donde tenga sentido.
