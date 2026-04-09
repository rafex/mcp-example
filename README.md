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
├── backend/
│   └── api-hello/
│       ├── java/
│       │   ├── README.md
│       │   └── src/
│       └── python/
│           ├── README.md
│           ├── hello_service.py
│           └── server.py
└── mcp/
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

Ejecutar MCP en Python:

```bash
just run-python-mcp-hello
```

Ejecutar MCP en Java:

```bash
just run-java-mcp-hello
```

## Propósito pedagógico

Este repositorio busca responder estas preguntas:

1. Cómo levantar una API HTTP mínima sin frameworks.
2. Cómo parsear parámetros y devolver JSON con herramientas nativas.
3. Cómo implementar MCP sobre `stdio` y JSON-RPC sin depender de SDKs.
4. Cómo mantener una idea funcional equivalente entre Python y Java.
5. Cómo documentar el proceso para que sirva como material de estudio.

## Próximos pasos

1. Extender `hello` con más validaciones y pruebas.
2. Añadir más ejemplos REST/MCP sobre otros casos simples.
3. Comparar una implementación totalmente estándar con una variante usando Ether donde tenga sentido.
