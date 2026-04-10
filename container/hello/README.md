# Container Hello

Esta carpeta contiene la forma de ejecutar los backends REST de `hello` dentro de contenedores.

La idea es poder probar:

- el backend Python sin instalar dependencias locales extra
- el backend Java sin tener Java instalado en la máquina anfitriona

## Archivos

- `Dockerfile.python-api`: imagen para `backend/api-hello/python`
- `Dockerfile.java-api`: imagen para `backend/api-hello/java`
- `compose.yaml`: levanta ambos servicios juntos

## Construir imágenes

Desde la raíz del repositorio:

```bash
just docker-build-python-api-hello
just docker-build-java-api-hello
```

## Ejecutar una imagen

Python:

```bash
just docker-run-python-api-hello
```

Java:

```bash
just docker-run-java-api-hello
```

## Levantar ambos servicios

```bash
just docker-up-hello
```

Para bajarlos:

```bash
just docker-down-hello
```

## Puertos

- Python API: `http://127.0.0.1:8080/hello`
- Java API: `http://127.0.0.1:8081/hello`

## Logs

Ambos contenedores montan la carpeta `logs/` de la raíz del repositorio en `/app/logs`, por lo que los logs siguen quedando visibles fuera del contenedor.
