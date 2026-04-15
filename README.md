# mcp-example

Proyecto de estudio para construir, comparar y entender:

1. APIs REST simples.
2. Servidores MCP paso a paso.
3. Implementaciones paralelas en Python y Java.

El repositorio está orientado a aprendizaje práctico: código pequeño, sin frameworks de aplicación, usando principalmente bibliotecas estándar. La única excepción permitida es [Ether](https://ether.rafex.io/) cuando aporte valor directo.

## Ejemplos actuales

Hoy el repositorio tiene tres casos de estudio:

- `hello`: saludo localizado sin autenticación
- `date`: consulta de hora por ubicación con autenticación HTTP escondida detrás del MCP
- `openweather`: acceso directo a OpenWeatherMap para clima actual y weather overview

Cada ejemplo incluye:

- un backend REST en Python
- un backend REST en Java

Los ejemplos MCP quedan así:

- `hello`: MCP manual en Python y Java, más variante adicional `hello-fastmcp` en Python
- `date`: MCP manual en Python y Java
- `openweather`: MCP manual en Python y Java, más variante adicional `openweather-fastmcp` en Python

Además, `hello` incluye una variante MCP adicional en Python usando FastMCP:

- `mcp-server/hello-fastmcp/python`

Además, `hello` incluye un agente Java que usa el MCP `hello` a traves de EtherBrain y DeepSeek.

Todos implementan la misma idea base:

- backend REST mínimo
- wrapper MCP del backend
- tools, resources y prompts para exponer la misma capacidad al cliente MCP

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

## Comportamiento de `openweather`

Los MCP `openweather` usan OpenWeatherMap directamente:

- Current Weather API 2.5
- Geocoding API
- One Call API 3.0 overview

Variables clave:

- `OPENWEATHER_API_KEY` obligatorio
- `OPENWEATHER_BASE_URL` opcional

Nota operativa:

- `get_weather_overview` depende de One Call API 3.0 overview
- en OpenWeatherMap esa capacidad puede requerir la suscripción correspondiente, además de una API key válida

## Estructura actual

```text
.
├── AGENTS.md
├── README.md
├── Makefile
├── justfile
├── mcp-client/
│   ├── README.md
│   ├── pyproject.toml
│   ├── uv.lock
│   └── main.py
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
│   ├── api-hello/
│   │   ├── java/
│   │   │   ├── README.md
│   │   │   └── src/
│   │   └── python/
│   │       ├── README.md
│   │       ├── hello_service.py
│   │       └── server.py
│   └── api-openweather/
│       ├── java/
│       │   ├── README.md
│       │   └── src/
│       └── python/
│           ├── README.md
│           ├── openweather_service.py
│           └── server.py
├── agents/
│   └── java/
│       └── agent-example-ether-brain/
│           ├── README.md
│           ├── pom.xml
│           └── src/
└── mcp-server/
    ├── docs/
    │   ├── README.md
    │   ├── what-is-mcp-es.md
    │   ├── what-is-mcp-en.md
    │   ├── stdio-vs-network-es.md
    │   ├── stdio-vs-network-en.md
    │   ├── mcp-server-es.md
    │   └── mcp-server-en.md
    ├── hello-fastmcp/
    │   └── python/
    │       ├── README.md
    │       ├── hello_api_client.py
    │       ├── requirements.txt
    │       └── server.py
    ├── openweather-fastmcp/
    │   └── python/
    │       ├── README.md
    │       ├── openweather_api_client.py
    │       ├── requirements.txt
    │       └── server.py
    ├── date/
    │   ├── java/
    │   │   ├── README.md
    │   │   └── src/
    │   └── python/
    │       ├── README.md
    │       └── server.py
    ├── hello/
    │   ├── java/
    │   │   ├── README.md
    │   │   └── src/
    │   └── python/
    │       ├── README.md
    │       ├── hello_service.py
    │       └── server.py
    └── openweather/
        ├── java/
        │   ├── README.md
        │   └── src/
        └── python/
            ├── README.md
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

Ejecutar API REST `openweather` en Python:

```bash
OPENWEATHER_API_KEY="tu_api_key" just run-python-api-openweather
```

Ejecutar API REST `openweather` en Java:

```bash
OPENWEATHER_API_KEY="tu_api_key" just run-java-api-openweather
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

Ejecutar MCP `hello-fastmcp` en Python:

```bash
just run-python-mcp-hello-fastmcp
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

Ejecutar MCP `openweather` en Python:

```bash
OPENWEATHER_API_KEY="tu_api_key" just run-python-mcp-openweather
```

Ejecutar MCP `openweather-fastmcp` en Python:

```bash
OPENWEATHER_API_KEY="tu_api_key" just run-python-mcp-openweather-fastmcp
```

Ejecutar MCP `openweather` en Java:

```bash
OPENWEATHER_API_KEY="tu_api_key" just run-java-mcp-openweather
```

Probar los MCP wrapper end-to-end:

```bash
./scripts/test-mcp-python.sh
./scripts/test-mcp-java.sh
./scripts/test-mcp-date-python.sh
./scripts/test-mcp-date-java.sh
./scripts/test-mcp-openweather-python.sh
./scripts/test-mcp-openweather-java.sh
```

Probar un MCP desde un cliente simple en la raíz y ver exactamente qué entrega:

```bash
cd mcp-client
uv sync
uv run python main.py hello python
uv run python main.py hello-fastmcp python
uv run python main.py hello java
uv run python main.py date python
uv run python main.py date java
uv run python main.py hello-fastmcp python --catalog-only --base-url http://127.0.0.1:8085
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather python --catalog-only
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather java --catalog-only
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather-fastmcp python --catalog-only
```

O usando la interfaz principal del proyecto:

```bash
just setup-mcp-client
just run-mcp-client hello python
just run-mcp-client date java
OPENWEATHER_API_KEY="tu_api_key" just run-mcp-client openweather python
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
- `hello-fastmcp` envuelve el mismo backend REST Python, pero publica las capacidades usando FastMCP
- el MCP Java envuelve al backend REST Java y expone tools, resources y prompts
- el agente Java usa el MCP Java como tool local

Hoy el ejemplo `date` funciona así:

- el backend REST exige autenticación por bearer token y `X-Date-Client`
- el backend REST resuelve la hora actual por ubicación y expone catálogo de ubicaciones
- el backend REST también expone datos para resources y prompts
- el MCP Python envuelve al backend REST Python y oculta la autenticación al cliente MCP
- el MCP Java envuelve al backend REST Java y oculta la autenticación al cliente MCP
- el cliente MCP solo ve tools, resources y prompts, no los headers del backend

Hoy el ejemplo `openweather` funciona así:

- el MCP Python manual habla directo con OpenWeatherMap
- `openweather-fastmcp` habla directo con OpenWeatherMap usando FastMCP
- el MCP Java manual habla directo con OpenWeatherMap
- el cliente MCP ve tools, resources y prompts sin conocer la API externa

## Cliente MCP de prueba

En la raíz del repositorio existe el proyecto `mcp-client/`, un cliente didáctico para inspeccionar los servidores MCP locales.

Este cliente:

- usa `uv`
- crea su propia `.venv`
- depende del SDK oficial Python de MCP
- compila Java cuando corresponde
- levanta el backend REST del ejemplo elegido en un puerto aislado cuando el ejemplo lo requiere
- arranca el servidor MCP correspondiente
- envía `initialize`
- lista `tools`, `resources` y `prompts`
- lee un resource
- obtiene un prompt
- ejecuta tools reales para mostrar el payload devuelto

Ejemplos:

```bash
cd mcp-client
uv sync
uv run python main.py hello python
uv run python main.py hello-fastmcp python --catalog-only
uv run python main.py hello java --catalog-only
uv run python main.py date python
uv run python main.py date java
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather python
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather java
OPENWEATHER_API_KEY="tu_api_key" uv run python main.py openweather-fastmcp python
```

Si ya tienes un backend levantado, por ejemplo el gateway Nginx en `http://127.0.0.1:8085`, puedes reutilizarlo con `--base-url` y el cliente no arrancará un backend aislado.

Para `openweather` y `openweather-fastmcp`, el cliente no necesita backend local porque ambos MCP hablan directo con OpenWeatherMap.

## Documentación MCP

La carpeta `mcp-server/docs/` contiene documentación conceptual para estudio.

Ahí se explica:

- qué es MCP
- cómo funciona MCP sobre `stdio`
- la diferencia entre `stdio` y transportes de red
- qué tipos de transporte puede usar un servidor MCP

## Próximos pasos

1. Añadir más pruebas automáticas a `hello` y `date`.
2. Crear un bridge HTTP para algún MCP `stdio` y poder probarlo desde Postman.
3. Comparar una implementación totalmente estándar con una variante usando Ether donde tenga sentido.
