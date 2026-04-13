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

### Nginx Gateway (Punto de Entrada Único con Balanceo de Carga)

**Puerto:** `8085`

El Nginx actúa como punto de entrada único y balanceador de carga con **Round Robin** entre los backends Python y Java.

#### Arquitectura de Balanceo

```
Client Request (8085)
    ↓
Nginx Gateway
    ↓
┌─────────────────┬─────────────────┐
│   Round Robin   │   Round Robin   │
│    Hello API    │    Date API     │
├─────────┬───────┴───────┬─────────┤
│ Python  │    Java       │ Python  │ Java
│ 8080    │    8081       │ 8090    │ 8091
└─────────┴───────────────┴─────────┴───────
```

#### Rutas Disponibles

| Ruta | Modo | Descripción |
|------|------|-------------|
| `/hello` | **Round Robin** | Distribuye peticiones entre Python y Java |
| `/hello/python` | Directo | Solo backend Python |
| `/hello/java` | Directo | Solo backend Java |
| `/date` | **Round Robin** | Distribuye peticiones entre Python y Java |
| `/date/python` | Directo | Solo backend Python |
| `/date/java` | Directo | Solo backend Java |
| `/hello/languages` | **Round Robin** | Lista idiomas soportados |
| `/date/time` | **Round Robin** | Obtiene hora actual |

#### Endpoints de Salud y Status

- **Health Check:** `http://localhost:8085/health`
- **Status:** `http://localhost:8085/status` (muestra configuración de balanceo)

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

### Balanceo de Carga Round Robin

Cuando usas las rutas base (`/hello` o `/date`), las peticiones se distribuyen automáticamente entre Python y Java usando Round Robin.

**Ejemplo 1: Hello API con balanceo**
```bash
# Ejecuta esto 4 veces y verás que las peticiones alternan entre Python y Java
curl http://localhost:8085/hello?name=Mundo&lang=es
```

**Ejemplo 2: Date API con balanceo**
```bash
# Ejecuta esto 4 veces y verás que las peticiones alternan entre Python y Java
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/time?location=mx-central
```

### Acceso Directo a Backends Específicos

Si necesitas probar un backend específico:

**Hello API Python:**
```bash
curl http://localhost:8085/hello/python?name=Mundo&lang=es
```

**Hello API Java:**
```bash
curl http://localhost:8085/hello/java?name=Mundo&lang=es
```

**Date API Python:**
```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/python/time?location=mx-central
```

**Date API Java:**
```bash
curl -H "Authorization: Bearer dev-date-token" \
     -H "X-Date-Client: mcp-date-client" \
     http://localhost:8085/date/java/time?location=us
```

### Verificar Headers Identificadores

Puedes verificar qué backend está respondiendo usando el header `X-Powered-By`:

```bash
# Ver header identificador
curl -I http://localhost:8085/hello?name=User

# Ver distribución de peticiones (Python vs Java)
for i in {1..10}; do
  curl -s -D - "http://localhost:8085/hello?name=User$i" -o /dev/null | grep "X-Powered-By"
done
```

### Ver Estado del Balanceo

```bash
curl http://localhost:8085/status
```

### Health Check

```bash
curl http://localhost:8085/health
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
