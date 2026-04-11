# mcp-example

Proyecto de estudio para construir, comparar y entender:

1. APIs REST simples.
2. Servidores MCP paso a paso.
3. Implementaciones paralelas en Python y Java.

El repositorio está orientado a aprendizaje práctico: código pequeño, sin frameworks de aplicación, usando principalmente bibliotecas estándar. La única excepción permitida es [Ether](https://ether.rafex.io/) cuando aporte valor directo.

## Ejemplos actuales

Hoy el repositorio tiene dos casos de estudio:

- `hello`: saludo localizado sin autenticación
- `date`: consulta de hora por ubicación con autenticación HTTP escondida detrás del MCP

Cada ejemplo incluye:

- un backend REST en Python
- un backend REST en Java
- un servidor MCP en Python
- un servidor MCP en Java

Además, `hello` incluye un agente Java que usa el MCP `hello` a traves de EtherBrain y DeepSeek.

Todos implementan la misma idea base:

- endpoint o herramienta `hello`
- parámetro opcional `name`
- parámetro opcional `lang`
- respuesta JSON con saludo, hora UTC e IP
- consulta de idiomas soportados
- catálogo de recursos MCP
- catálogo de prompts MCP

## Comportamiento de `hello`

Ruta REST:

- `GET /hello`
- `POST /hello`
- `OPTIONS /hello`
- `GET /hello/languages`
- `OPTIONS /hello/languages`

Parámetros soportados:

- `name`: nombre opcional de la persona a saludar
- `lang`: idioma del saludo, por ejemplo `en` o `es`

La respuesta cambia si se envía `name` o no.

Además, el backend REST expone `GET /hello/languages` para devolver la cantidad total de idiomas soportados y sus códigos.

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

## Comportamiento de `date`

Rutas REST:

- `GET /date/time`
- `GET /date/locations`
- `GET /date/resources`
- `GET /date/resources/{name}`
- `GET /date/prompts`
- `GET /date/prompts/{name}`
- `OPTIONS` para todas esas rutas

El backend `date` requiere autenticación por headers:

- `Authorization: Bearer <token>`
- `X-Date-Client: <client-id>`

Su objetivo didáctico es mostrar un caso donde el backend REST exige detalles operativos que no conviene exponer al modelo o al agente. Por eso el MCP `date` actúa como wrapper y oculta esos headers.

`date` soporta 53 ubicaciones:

- 50 países relevantes
- 3 zonas horarias de México: `mx-central`, `mx-northwest`, `mx-southeast`

Los tools actuales del MCP `date` son:

- `get_current_time`
- `list_supported_locations`

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
│   ├── api-date.yaml
│   ├── api-hello.yaml
│   └── mcp-hello.http.json
├── backend/
│   ├── api-date/
│   │   ├── java/
│   │   │   ├── README.md
│   │   │   └── src/
│   │   └── python/
│   │       ├── README.md
│   │       ├── date_service.py
│   │       └── server.py
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
    ├── date/
    │   ├── java/
    │   │   ├── README.md
    │   │   └── src/
    │   └── python/
    │       ├── README.md
    │       └── server.py
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

Ejecutar API REST `date` en Python:

```bash
just run-python-api-date
```

Ejecutar API REST `date` en Java:

```bash
just run-java-api-date
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

Ejecutar MCP `date` en Python:

```bash
just run-python-mcp-date
```

Ejecutar MCP `date` en Java:

```bash
just run-java-mcp-date
```

Probar los MCP wrapper end-to-end:

```bash
./scripts/test-mcp-python.sh
./scripts/test-mcp-java.sh
./scripts/test-mcp-date-python.sh
./scripts/test-mcp-date-java.sh
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

## Estado actual de los ejemplos

Hoy el ejemplo `hello` funciona así:

- el backend REST resuelve el saludo y la lista de idiomas
- el backend REST también expone datos para resources y prompts
- el MCP Python envuelve al backend REST Python y expone tools, resources y prompts
- el MCP Java envuelve al backend REST Java y expone tools, resources y prompts
- el agente Java usa el MCP Java como tool local

Hoy el ejemplo `date` funciona así:

- el backend REST exige autenticación por bearer token y `X-Date-Client`
- el backend REST resuelve la hora actual por ubicación y expone catálogo de ubicaciones
- el backend REST también expone datos para resources y prompts
- el MCP Python envuelve al backend REST Python y oculta la autenticación al cliente MCP
- el MCP Java envuelve al backend REST Java y oculta la autenticación al cliente MCP
- el cliente MCP solo ve tools, resources y prompts, no los headers del backend

## Documentación MCP

La carpeta `mcp/docs/` contiene documentación conceptual para estudio.

Ahí se explica:

- qué es MCP
- cómo funciona MCP sobre `stdio`
- la diferencia entre `stdio` y transportes de red
- qué tipos de transporte puede usar un servidor MCP

## Próximos pasos

1. Añadir más pruebas automáticas a `hello` y `date`.
2. Crear un bridge HTTP para algún MCP `stdio` y poder probarlo desde Postman.
3. Comparar una implementación totalmente estándar con una variante usando Ether donde tenga sentido.
