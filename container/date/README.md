# Container Date API

Contiene los Dockerfiles para los servicios Date API (Python y Java).

## Archivos

- `Dockerfile.python-api`: Imagen para `backend/api-date/python`
- `Dockerfile.java-api`: Imagen para `backend/api-date/java`

## Uso

Este directorio es parte de la orquestación completa en `container/docker-compose.yml`.

### Construir individualmente

```bash
docker build -f container/date/Dockerfile.python-api -t mcp-example/date-python-api:latest .
docker build -f container/date/Dockerfile.java-api -t mcp-example/date-java-api:latest .
```

### Ejecutar individualmente (sin compose)

**Python API:**
```bash
docker run -d -p 8090:8090 \
  -e DATE_API_TOKEN=dev-date-token \
  -e DATE_API_CLIENT_ID=mcp-date-client \
  -v $(pwd)/logs:/app/logs \
  mcp-example/date-python-api:latest
```

**Java API:**
```bash
docker run -d -p 8091:8091 \
  -e DATE_API_TOKEN=dev-date-token \
  -e DATE_API_CLIENT_ID=mcp-date-client \
  -v $(pwd)/logs:/app/logs \
  mcp-example/date-java-api:latest
```

## Endpoints

### Python API
- URL: `http://localhost:8090/date/`
- Requiere autenticación: Headers `Authorization` y `X-Date-Client`

### Java API
- URL: `http://localhost:8091/date/`
- Requiere autenticación: Headers `Authorization` y `X-Date-Client`

## A través de Nginx (recomendado)

Usa el gateway unificado en `http://localhost:8085/`:
- Date Python: `http://localhost:8085/date/python/`
- Date Java: `http://localhost:8085/date/java/`
- Date (default): `http://localhost:8085/date/`

## Autenticación

Todos los endpoints de Date API requieren autenticación:

```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/time?location=mx-central
```

### Variables de Entorno

Puedes configurar el token y cliente mediante variables de entorno:

- `DATE_API_TOKEN`: Token de autenticación (default: `dev-date-token`)
- `DATE_API_CLIENT_ID`: ID de cliente (default: `mcp-date-client`)

## Logs

Los logs se escriben en:
- Contenedor: `/app/logs/`
- Host: `$(pwd)/logs/` (montado desde la raíz del repositorio)

Archivos:
- `backend-api-date-python.log`
- `backend-api-date-java.log`

## Ejemplos de Uso

### Obtener hora actual (Python)
```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/time?location=mx-central
```

### Obtener hora actual (Java)
```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/java/time?location=us
```

### Listar ubicaciones soportadas
```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/locations
```
