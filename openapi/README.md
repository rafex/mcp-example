# OpenAPI

Esta carpeta concentra las definiciones de interfaz del repositorio para poder explorar y probar los ejemplos con herramientas como Bruno o Postman.

## Criterio de organización

- Los APIs REST se describen con archivos OpenAPI `yaml`.
- Los MCP se acompañan con ejemplos JSON-RPC y documentación de uso.

## Por qué no todo es OpenAPI

OpenAPI modela muy bien APIs HTTP.

Los ejemplos MCP actuales de este repositorio usan `stdio` y JSON-RPC, no HTTP. Eso significa:

- no hay una URL REST que Postman o Bruno puedan consumir directamente
- no existe una operación HTTP equivalente para describir fielmente el transporte actual

Por eso, para MCP guardamos aquí artefactos de referencia útiles para estudio e integración:

- ejemplos de `initialize`
- ejemplos de `tools/list`
- ejemplos de `tools/call`
- colecciones o requests serializados si hacen falta

## Archivos actuales

- `api-hello.yaml`: definición OpenAPI del endpoint REST `GET /hello`
- `mcp-hello.http.json`: ejemplos JSON-RPC del MCP `hello`

## Convención futura

Por cada nuevo ejemplo del repositorio, esta carpeta debería incluir:

1. una definición OpenAPI para la parte REST
2. ejemplos JSON-RPC para la parte MCP
3. si existe una variante MCP sobre HTTP, su definición OpenAPI correspondiente
