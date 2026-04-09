# API Hello Python

Backend REST mínimo en Python usando solo biblioteca estándar.

## Endpoint

- `GET /hello`

## Query params

- `name`: opcional
- `lang`: opcional, por defecto `en`

## Ejecutar

Desde la raíz del repositorio:

```bash
just run-python-api-hello
```

O directamente:

```bash
python3 backend/api-hello/python/server.py
```

## Probar

Sin nombre:

```bash
curl "http://127.0.0.1:8080/hello"
```

Con nombre e idioma:

```bash
curl "http://127.0.0.1:8080/hello?name=Ra%C3%BAl&lang=es"
```

## Implementación

- `server.py` implementa el servidor HTTP con `http.server`
- `hello_service.py` concentra la lógica del saludo
