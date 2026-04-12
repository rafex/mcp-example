# Container del Proyecto MCP Example

Esta carpeta contiene la configuración de contenedores Docker para ejecutar todos los servicios del proyecto.

## Estructura

```
container/
├── hello/                    # Servicios Hello API
│   ├── Dockerfile.python-api
│   ├── Dockerfile.java-api
│   └── README.md
├── date/                     # Servicios Date API
│   ├── Dockerfile.python-api
│   └── Dockerfile.java-api
├── nginx/                    # Nginx Gateway
│   ├── Dockerfile
│   └── nginx.conf
└── docker-compose.yml        # Orquestación completa
```

## Servicios Disponibles

### Nginx Gateway (Punto de Entrada Único)

**Puerto:** `8085`

El Nginx actúa como punto de entrada único para todos los servicios:

| Servicio | Ruta Proxy | Backend Real |
|----------|------------|--------------|
| Hello Python | `/hello/python/` | `hello-python-api:8080` |
| Hello Java | `/hello/java/` | `hello-java-api:8081` |
| Hello (default) | `/hello/` | `hello-python-api:8080` |
| Date Python | `/date/python/` | `date-python-api:8090` |
| Date Java | `/date/java/` | `date-java-api:8091` |
| Date (default) | `/date/` | `date-python-api:8090` |

### Endpoints de Salud

- **Health Check:** `http://localhost:8085/health`

## Uso

### 1. Construir todas las imágenes

Desde la raíz del repositorio:

```bash
# Construir todas las imágenes
docker-compose -f container/docker-compose.yml build

# O usar los comandos de just (si están configurados)
just docker-build-all
```

### 2. Levantar todos los servicios

```bash
docker-compose -f container/docker-compose.yml up -d
```

### 3. Ver logs de todos los servicios

```bash
docker-compose -f container/docker-compose.yml logs -f
```

### 4. Ver logs de un servicio específico

```bash
docker-compose -f container/docker-compose.yml logs -f nginx
docker-compose -f container/docker-compose.yml logs -f hello-python-api
```

### 5. Detener todos los servicios

```bash
docker-compose -f container/docker-compose.yml down
```

### 6. Levantar servicios individuales

Si solo necesitas algunos servicios:

```bash
# Solo Hello API (Python y Java)
docker-compose -f container/docker-compose.yml up -d hello-python-api hello-java-api

# Solo Date API (Python y Java)
docker-compose -f container/docker-compose.yml up -d date-python-api date-java-api

# Solo Nginx
docker-compose -f container/docker-compose.yml up -d nginx
```

## Ejemplos de Uso

### Hello API (Python - Default)

```bash
curl http://localhost:8085/hello/
# o especificando el lenguaje
curl http://localhost:8085/hello/?name=Mundo&lang=es
```

### Hello API (Java)

```bash
curl http://localhost:8085/hello/java/
```

### Date API (Python - Default)

```bash
# Requiere autenticación
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/time?location=mx-central
```

### Date API (Java)

```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/java/time?location=us
```

## Variables de Entorno

### Date API

Las siguientes variables de entorno pueden configurarse en el archivo `.env` del repositorio raíz:

- `DATE_API_TOKEN`: Token de autenticación (default: `dev-date-token`)
- `DATE_API_CLIENT_ID`: ID de cliente (default: `mcp-date-client`)

Ejemplo de archivo `.env`:
```bash
DATE_API_TOKEN=mipasswordseguro
DATE_API_CLIENT_ID=mi-cliente-id
```

## Logs

Todos los contenedores montan la carpeta `logs/` de la raíz del repositorio en `/app/logs`, por lo que los logs están disponibles tanto dentro como fuera del contenedor.

- Logs de Hello Python: `logs/backend-api-hello-python.log`
- Logs de Hello Java: `logs/backend-api-hello-java.log`
- Logs de Date Python: `logs/backend-api-date-python.log`
- Logs de Date Java: `logs/backend-api-date-java.log`

## Debugging

### Ver logs en tiempo real

```bash
# Todos los servicios
docker-compose -f container/docker-compose.yml logs -f

# Solo Nginx
docker-compose -f container/docker-compose.yml logs -f nginx

# Solo un backend específico
docker-compose -f container/docker-compose.yml logs -f hello-python-api
```

### Ejecutar en modo foreground (sin detach)

```bash
docker-compose -f container/docker-compose.yml up
```

### Entrar a un contenedor

```bash
# Hello Python API
docker-compose -f container/docker-compose.yml exec hello-python-api sh

# Hello Java API
docker-compose -f container/docker-compose.yml exec hello-java-api sh

# Nginx
docker-compose -f container/docker-compose.yml exec nginx sh
```

### Probar servicios directamente (sin Nginx)

```bash
# Hello Python API directo
curl http://localhost:8080/hello/

# Hello Java API directo
curl http://localhost:8081/hello/

# Date Python API directo
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8090/date/time?location=us
```

## Notas

- Todos los servicios usan el mismo network `backend-network` para comunicarse internamente
- Nginx actúa como balanceador de carga y punto de entrada único
- Los puertos de los backends individuales no están expuestos al host por defecto (solo Nginx en 8085)
- Para acceder a backends individuales, se pueden exponer puertos adicionales en el docker-compose.yml
