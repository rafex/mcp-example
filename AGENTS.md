# AGENTS.md

## Propósito

Este archivo define reglas de trabajo para agentes y colaboradores de este repositorio.

El proyecto enseña cómo construir APIs REST y servidores MCP en Python y Java con ejemplos pequeños, equivalentes y principalmente basados en bibliotecas estándar.

## Ejemplo actual

El primer ejemplo oficial es `hello`.

Su estructura objetivo es:

- `backend/api-hello/python`
- `backend/api-hello/java`
- `mcp/hello/python`
- `mcp/hello/java`

Cada implementación debe mantener la misma intención funcional:

- saludo opcional con `name`
- selección de idioma con `lang`
- respuesta con `message`, `timestamp`, `ip`

## Restricciones técnicas

- No usar frameworks de aplicación para REST.
- No usar frameworks de aplicación para MCP.
- Preferir bibliotecas estándar de Python y Java.
- La única excepción prevista es [Ether](https://ether.rafex.io/) si se decide documentarlo explícitamente.
- Mantener dependencias al mínimo.

## Convenciones de arquitectura

- Separar lógica de saludo de la capa de transporte cuando sea razonable.
- Mantener equivalencia conceptual entre Python y Java.
- Mantener equivalencia conceptual entre REST y MCP.
- Evitar abstracciones innecesarias en ejemplos pedagógicos.
- Priorizar código legible antes que código genérico.

## Convenciones de build y tareas

- `justfile` es la interfaz principal para desarrollo.
- `Makefile` es la base reusable para build y ejecución.
- `just` puede llamar a `make`.
- `make` no debe llamar a `just`.

## Convenciones de documentación

- Cada ejemplo importante debe tener su propio `README.md`.
- Los directorios MCP deben incluir un `README.md` detallado y didáctico.
- La documentación debe explicar el "qué", el "por qué" y el "cómo ejecutarlo".
- Si un cambio altera la estructura o los comandos, actualizar la documentación en el mismo cambio.

## Convenciones de implementación

- Preferir código pequeño y explícito.
- Añadir comentarios solo cuando aclaren algo no evidente.
- Mantener nombres de archivos y carpetas consistentes.
- Evitar duplicación innecesaria, pero no sacrificar claridad pedagógica por DRY extremo.

## Qué debe hacer un agente

Antes de editar:

- revisar la estructura actual del ejemplo afectado
- validar si el cambio debe reflejarse en Python y Java
- validar si el cambio también impacta la versión MCP

Durante la edición:

- mantener cambios pequeños y trazables
- no introducir frameworks sin dejarlo documentado
- no romper la relación `just -> make`

Después de editar:

- verificar compilación o ejecución básica de lo tocado
- revisar que la documentación siga siendo correcta

## Estructura de Contenedores

El proyecto incluye una infraestructura Docker completa con Nginx como balanceador de carga:

### Balanceo de Carga Round Robin

- **Hello API**: Ruta `/hello` distribuye peticiones entre Python (`:8080`) y Java (`:8081`)
- **Date API**: Ruta `/date` distribuye peticiones entre Python (`:8090`) y Java (`:8091`)

### Endpoints Disponibles

| Ruta | Modo | Backends |
|------|------|----------|
| `/hello` | Round Robin | Python + Java |
| `/hello/python` | Directo | Solo Python |
| `/hello/java` | Directo | Solo Java |
| `/date` | Round Robin | Python + Java |
| `/date/python` | Directo | Solo Python |
| `/date/java` | Directo | Solo Java |

### Headers Identificadores

Cada backend envía un header `X-Powered-By` para identificar el lenguaje:
- Python: `X-Powered-By: Python`
- Java: `X-Powered-By: Java` (o `X-powered-by: Java` según Nginx)

### Comandos Docker

```bash
# Construir todas las imágenes
just docker-build-all

# Levantar todos los servicios
just docker-up-all

# Ver logs
just docker-logs

# Health check
curl http://localhost:8085/health

# Status del balanceo
curl http://localhost:8085/status
```

## Prioridades

Orden de prioridad:

1. Claridad didáctica.
2. Simetría entre implementaciones.
3. Simplicidad operativa.
4. Dependencias mínimas.
5. Extensibilidad razonable.
