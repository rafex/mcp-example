# API Hello Python

Backend REST mínimo en Python usando solo biblioteca estándar.

## Endpoint

- `GET /hello`
- `POST /hello`
- `OPTIONS /hello`
- `GET /hello/languages`
- `OPTIONS /hello/languages`

## Entrada

Para `GET`:

- `name`: opcional
- `lang`: opcional, por defecto `en`

Para `POST`:

- cuerpo JSON opcional con `name` y `lang`

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

Con `POST`:

```bash
curl -X POST "http://127.0.0.1:8080/hello" \
  -H "Content-Type: application/json" \
  --data '{"name":"Raúl","lang":"en"}'
```

Ver métodos soportados:

```bash
curl -i -X OPTIONS "http://127.0.0.1:8080/hello"
```

Consultar idiomas soportados:

```bash
curl "http://127.0.0.1:8080/hello/languages"
```

Ver métodos soportados para idiomas:

```bash
curl -i -X OPTIONS "http://127.0.0.1:8080/hello/languages"
```

## Implementación

- `server.py` implementa el servidor HTTP con `http.server`
- `hello_service.py` concentra la lógica del saludo y la metadata de idiomas
