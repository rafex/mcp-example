# API Hello Java

Backend REST mínimo en Java usando biblioteca estándar.

## Endpoint

- `GET /hello`

## Query params

- `name`: opcional
- `lang`: opcional, por defecto `en`

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

## Implementación

- `src/HelloApiServer.java` implementa el servidor HTTP con `com.sun.net.httpserver.HttpServer`
- `src/HelloService.java` concentra la lógica del saludo
