# API Hello Java

Backend REST mínimo en Java usando biblioteca estándar.

## Endpoint

- `GET /hello`
- `POST /hello`
- `OPTIONS /hello`

## Entrada

Para `GET`:

- `name`: opcional
- `lang`: opcional, por defecto `en`

Para `POST`:

- cuerpo JSON opcional con `name` y `lang`

## Ejecutar

Desde la raíz del repositorio:

```bash
just run-java-api-hello
```

O en dos pasos:

```bash
make build-java-api-hello
java -cp backend/api-hello/java/build HelloApiServer
```

## Probar

Sin nombre:

```bash
curl "http://127.0.0.1:8081/hello"
```

Con nombre e idioma:

```bash
curl "http://127.0.0.1:8081/hello?name=Ra%C3%BAl&lang=es"
```

Con `POST`:

```bash
curl -X POST "http://127.0.0.1:8081/hello" \
  -H "Content-Type: application/json" \
  --data '{"name":"Raúl","lang":"es"}'
```

Ver métodos soportados:

```bash
curl -i -X OPTIONS "http://127.0.0.1:8081/hello"
```

## Implementación

- `src/HelloApiServer.java` implementa el servidor HTTP con `com.sun.net.httpserver.HttpServer`
- `src/HelloService.java` concentra la lógica del saludo
