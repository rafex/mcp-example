run-python-api-hello:
    make run-python-api-hello

build-java-api-hello:
    make build-java-api-hello

run-java-api-hello:
    make run-java-api-hello

run-python-mcp-hello:
    make run-python-mcp-hello

build-java-mcp-hello:
    make build-java-mcp-hello

run-java-mcp-hello:
    make run-java-mcp-hello

run-python-api-date:
    make run-python-api-date

build-java-api-date:
    make build-java-api-date

run-java-api-date:
    make run-java-api-date

run-python-mcp-date:
    make run-python-mcp-date

build-java-mcp-date:
    make build-java-mcp-date

run-java-mcp-date:
    make run-java-mcp-date

build-java-agent-example-ether-brain:
    make build-java-agent-example-ether-brain

run-java-agent-example-ether-brain:
    make build-java-agent-example-ether-brain
    mvn -q -f agents/java/agent-example-ether-brain/pom.xml exec:java -Dexec.args="${PROMPT:-Usa la tool hello_mcp para saludar a Ada Lovelace en es y responde breve.}"

run-java-agent-example-ether-brain-check-mcp:
    make build-java-agent-example-ether-brain
    mvn -q -f agents/java/agent-example-ether-brain/pom.xml exec:java -Dexec.args="--check-mcp"

run-java-agent-example-ether-brain-check-mcp-languages:
    make build-java-agent-example-ether-brain
    mvn -q -f agents/java/agent-example-ether-brain/pom.xml exec:java -Dexec.args="--check-mcp-languages"

docker-build-python-api-hello:
    make docker-build-python-api-hello

docker-run-python-api-hello:
    make docker-run-python-api-hello

docker-build-java-api-hello:
    make docker-build-java-api-hello

docker-run-java-api-hello:
    make docker-run-java-api-hello

docker-up-hello:
    make docker-up-hello

docker-down-hello:
    make docker-down-hello

# Docker compose commands
docker-build-all:
    docker-compose -f container/docker-compose.yml build

docker-up-all:
    docker-compose -f container/docker-compose.yml up -d

docker-down-all:
    docker-compose -f container/docker-compose.yml down

docker-logs:
    docker-compose -f container/docker-compose.yml logs -f

docker-logs-nginx:
    docker-compose -f container/docker-compose.yml logs -f nginx

docker-build-nginx:
    docker build -f container/nginx/Dockerfile -t mcp-example/nginx-gateway:latest .

docker-up-nginx:
    docker-compose -f container/docker-compose.yml up -d nginx

# Date API Docker commands
docker-build-python-api-date:
    docker build -f container/date/Dockerfile.python-api -t mcp-example/date-python-api:latest .

docker-build-java-api-date:
    docker build -f container/date/Dockerfile.java-api -t mcp-example/date-java-api:latest .

docker-up-date:
    docker-compose -f container/docker-compose.yml up -d date-python-api date-java-api

# Individual service commands
docker-up-hello-python:
    docker-compose -f container/docker-compose.yml up -d hello-python-api

docker-up-hello-java:
    docker-compose -f container/docker-compose.yml up -d hello-java-api

docker-up-date-python:
    docker-compose -f container/docker-compose.yml up -d date-python-api

docker-up-date-java:
    docker-compose -f container/docker-compose.yml up -d date-java-api
