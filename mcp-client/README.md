# mcp-client

Proyecto cliente en la raíz del repositorio para probar todos los servidores MCP de este repo.

Usa `uv`, crea su propia `.venv` y depende del SDK oficial Python de MCP.

## Objetivo

Este proyecto sirve para inspeccionar y probar:

- `mcp-server/hello/python`
- `mcp-server/hello/java`
- `mcp-server/hello-fastmcp/python`
- `mcp-server/date/python`
- `mcp-server/date/java`
- `mcp-server/openweather/python`
- `mcp-server/openweather/java`

El cliente:

- levanta el backend REST necesario en un puerto aislado
- compila Java cuando hace falta
- arranca el servidor MCP correspondiente
- muestra `tools/list`, `resources/list` y `prompts/list`
- puede leer un resource
- puede obtener un prompt
- puede ejecutar tools reales y mostrar el payload resultante

## Dependencia oficial

El SDK oficial Python de MCP se instala hoy como:

```bash
uv add "mcp[cli]"
```

o, dentro de la `.venv`:

```bash
uv pip install "mcp[cli]"
```

Nota: aunque a veces se busque como “modelcontextprotocol”, el paquete oficial publicado para Python es `mcp`.

## Preparación

Desde la raíz del repositorio:

```bash
cd mcp-client
uv sync
```

Eso crea:

- `.venv/`
- `uv.lock`

## Uso

Desde `mcp-client/`:

```bash
uv run python main.py hello python
uv run python main.py hello java
uv run python main.py hello-fastmcp python
uv run python main.py date python
uv run python main.py date java
uv run python main.py openweather python
uv run python main.py openweather java
```

Solo catálogo:

```bash
uv run python main.py hello python --catalog-only
uv run python main.py hello-fastmcp python --catalog-only
```

Reutilizar un backend ya levantado, por ejemplo el gateway en `http://127.0.0.1:8085`:

```bash
uv run python main.py hello-fastmcp python --catalog-only --base-url http://127.0.0.1:8085
```

Si usas `--base-url`, el cliente no levanta backend local.

## Diseño del cliente

Este proyecto usa dos estrategias de transporte:

- `manual`: para los ejemplos MCP del repo implementados a mano con cabeceras `Content-Length`
- `sdk`: para `hello-fastmcp`, usando `ClientSession` y `stdio_client` del SDK oficial

Eso permite probar tanto:

- implementaciones didácticas sin SDK
- implementaciones basadas en FastMCP

## Archivos principales

- `main.py`: CLI del cliente
- `pyproject.toml`: definición del proyecto `uv`
- `uv.lock`: lockfile reproducible

## Salida esperada

Cada ejecución imprime secciones como:

- `tools/list`
- `resources/list`
- `prompts/list`
- `resources/read`
- `prompts/get`
- `tools/call:<nombre>`

La salida se muestra en JSON legible para revisar exactamente qué entrega cada servidor.
