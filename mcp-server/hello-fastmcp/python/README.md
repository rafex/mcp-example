# MCP Hello FastMCP Python

Servidor MCP `hello-fastmcp` en Python usando FastMCP, el SDK oficial de MCP para Python.

Este ejemplo existe como contraste pedagógico frente a `mcp-server/hello/python`, que implementa el protocolo manualmente por `stdio`.

## Por qué existe

El repositorio prioriza implementaciones pequeñas con biblioteca estándar. Este directorio es una excepción deliberada y documentada para comparar dos enfoques:

- `mcp-server/hello/python`: implementación manual de MCP sin SDK
- `mcp-server/hello-fastmcp/python`: implementación equivalente usando FastMCP

La intención funcional es la misma:

- tool `say_hello`
- tool `get_hello_languages`
- 2 resources
- 2 prompts

## Dependencia

FastMCP forma parte actualmente del SDK oficial `mcp` para Python.

Según la documentación oficial del SDK:

- instalación con `pip install "mcp[cli]"`
- import principal con `from mcp.server.fastmcp import FastMCP`

Para este ejemplo:

```bash
python3 -m pip install -r mcp-server/hello-fastmcp/python/requirements.txt
```

## Arquitectura

Este servidor no resuelve el saludo directamente.

Igual que el ejemplo MCP `hello` manual, actúa como wrapper del backend REST `hello`:

- `say_hello` llama `GET /hello`
- `get_hello_languages` llama `GET /hello/languages`
- los resources leen `GET /hello/resources/...`
- los prompts reutilizan `GET /hello/prompts/...`

## Archivos

- `server.py`: servidor FastMCP
- `hello_api_client.py`: cliente HTTP mínimo hacia el backend REST
- `requirements.txt`: dependencia explícita del ejemplo

## Ejecutar

Primero levanta el backend REST `hello`:

```bash
just run-python-api-hello
```

Luego, en otra terminal:

```bash
just run-python-mcp-hello-fastmcp
```

O directamente:

```bash
python3 mcp-server/hello-fastmcp/python/server.py
```

El backend REST por defecto se busca en:

```text
http://127.0.0.1:8085
```

Puedes cambiarlo con:

```bash
HELLO_API_BASE_URL=http://127.0.0.1:8080 python3 mcp-server/hello-fastmcp/python/server.py
```

## Qué expone

### Tools

- `say_hello`
- `get_hello_languages`

### Resources

- `hello://service-overview`
- `hello://language-reference`

### Prompts

FastMCP deriva los nombres de prompt desde las funciones Python:

- `greet_user`
- `language_report`

## Cómo probarlo

Si tienes instalada la dependencia, puedes usar el cliente MCP del repositorio:

```bash
python3 mcp-client.py hello-fastmcp python
```

O solo inspeccionar catálogo:

```bash
python3 mcp-client.py hello-fastmcp python --catalog-only
```

## Qué comparar con el ejemplo manual

- cuánto código desaparece al delegar framing y JSON-RPC al SDK
- cómo FastMCP publica tools, resources y prompts desde decoradores
- qué parte del ejemplo sigue igual: la lógica de negocio y la llamada al backend REST
