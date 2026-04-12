# Container Hello API

Contiene los Dockerfiles para los servicios Hello API (Python y Java).

## Archivos

- `Dockerfile.python-api`: Imagen para `backend/api-hello/python`
- `Dockerfile.java-api`: Imagen para `backend/api-hello/java`

## Uso

Este directorio es parte de la orquestación completa en `container/docker-compose.yml`.

### Construir individualmente

```bash
docker build -f container/hello/Dockerfile.python-api -t mcp-example/hello-python-api:latest .
docker build -f container/hello/Dockerfile.java-api -t mcp-example/hello-java-api:latest .
```

### Ejecutar individualmente (sin compose)

**Python API:**
```bash
docker run -d -p 8080:8080 \
  -v $(pwd)/logs:/app/logs \
  mcp-example/hello-python-api:latest
```

**Java API:**
```bash
docker run -d -p 8081:8081 \
  -v $(pwd)/logs:/app/logs \
  mcp-example/hello-java-api:latest
```

## Endpoints

### Python API
- URL: `http://localhost:8080/hello/`
- Health: `http://localhost:8080/hello/`

### Java API
- URL: `http://localhost:8081/hello/`
- Health: `http://localhost:8081/hello/`

## A través de Nginx (recomendado)

Usa el gateway unificado en `http://localhost:8085/`:
- Hello Python: `http://localhost:8085/hello/python/`
- Hello Java: `http://localhost:8085/hello/java/`
- Hello (default): `http://localhost:8085/hello/`

## Logs

Los logs se escriben en:
- Contenedor: `/app/logs/`
- Host: `$(pwd)/logs/` (montado desde la raíz del repositorio)

Archivos:
- `backend-api-hello-python.log`
- `backend-api-hello-java.log`
