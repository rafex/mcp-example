PYTHON_API_HELLO_DIR := backend/api-hello/python
JAVA_API_HELLO_DIR := backend/api-hello/java
PYTHON_MCP_HELLO_DIR := mcp-server/hello/python
PYTHON_MCP_HELLO_FASTMCP_DIR := mcp-server/hello-fastmcp/python
JAVA_MCP_HELLO_DIR := mcp-server/hello/java
PYTHON_API_DATE_DIR := backend/api-date/python
JAVA_API_DATE_DIR := backend/api-date/java
PYTHON_API_OPENWEATHER_DIR := backend/api-openweather/python
JAVA_API_OPENWEATHER_DIR := backend/api-openweather/java
PYTHON_MCP_DATE_DIR := mcp-server/date/python
JAVA_MCP_DATE_DIR := mcp-server/date/java
PYTHON_MCP_OPENWEATHER_DIR := mcp-server/openweather/python
PYTHON_MCP_OPENWEATHER_FASTMCP_DIR := mcp-server/openweather-fastmcp/python
JAVA_MCP_OPENWEATHER_DIR := mcp-server/openweather/java
JAVA_AGENT_EXAMPLE_ETHER_BRAIN_DIR := agents/java/agent-example-ether-brain
CONTAINER_HELLO_DIR := container/hello
JAVA_API_HELLO_BUILD_DIR := $(JAVA_API_HELLO_DIR)/build
JAVA_MCP_HELLO_BUILD_DIR := $(JAVA_MCP_HELLO_DIR)/build
JAVA_API_DATE_BUILD_DIR := $(JAVA_API_DATE_DIR)/build
JAVA_MCP_DATE_BUILD_DIR := $(JAVA_MCP_DATE_DIR)/build
JAVA_API_OPENWEATHER_BUILD_DIR := $(JAVA_API_OPENWEATHER_DIR)/build
JAVA_MCP_OPENWEATHER_BUILD_DIR := $(JAVA_MCP_OPENWEATHER_DIR)/build
UV_BIN ?= $(HOME)/.local/bin/uv

.PHONY: run-python-api-hello build-java-api-hello run-java-api-hello run-python-mcp-hello run-python-mcp-hello-fastmcp build-java-mcp-hello run-java-mcp-hello run-python-api-date build-java-api-date run-java-api-date run-python-mcp-date build-java-mcp-date run-java-mcp-date run-python-api-openweather build-java-api-openweather run-java-api-openweather run-python-mcp-openweather run-python-mcp-openweather-fastmcp build-java-mcp-openweather run-java-mcp-openweather setup-mcp-client run-mcp-client build-java-agent-example-ether-brain docker-build-python-api-hello docker-run-python-api-hello docker-build-java-api-hello docker-run-java-api-hello docker-up-hello docker-down-hello

run-python-api-hello:
	python3 $(PYTHON_API_HELLO_DIR)/server.py

build-java-api-hello:
	mkdir -p $(JAVA_API_HELLO_BUILD_DIR)
	javac -d $(JAVA_API_HELLO_BUILD_DIR) $(JAVA_API_HELLO_DIR)/src/*.java

run-java-api-hello: build-java-api-hello
	java -cp $(JAVA_API_HELLO_BUILD_DIR) HelloApiServer

run-python-mcp-hello:
	python3 $(PYTHON_MCP_HELLO_DIR)/server.py

run-python-mcp-hello-fastmcp:
	python3 $(PYTHON_MCP_HELLO_FASTMCP_DIR)/server.py

build-java-mcp-hello:
	mkdir -p $(JAVA_MCP_HELLO_BUILD_DIR)
	javac -d $(JAVA_MCP_HELLO_BUILD_DIR) $(JAVA_MCP_HELLO_DIR)/src/*.java

run-java-mcp-hello: build-java-mcp-hello
	java -cp $(JAVA_MCP_HELLO_BUILD_DIR) HelloMcpServer

run-python-api-date:
	python3 $(PYTHON_API_DATE_DIR)/server.py

build-java-api-date:
	mkdir -p $(JAVA_API_DATE_BUILD_DIR)
	javac -d $(JAVA_API_DATE_BUILD_DIR) $(JAVA_API_DATE_DIR)/src/*.java

run-java-api-date: build-java-api-date
	java -cp $(JAVA_API_DATE_BUILD_DIR) DateApiServer

run-python-api-openweather:
	python3 $(PYTHON_API_OPENWEATHER_DIR)/server.py

build-java-api-openweather:
	mkdir -p $(JAVA_API_OPENWEATHER_BUILD_DIR)
	javac -d $(JAVA_API_OPENWEATHER_BUILD_DIR) $(JAVA_API_OPENWEATHER_DIR)/src/*.java

run-java-api-openweather: build-java-api-openweather
	java -cp $(JAVA_API_OPENWEATHER_BUILD_DIR) OpenWeatherApiServer

run-python-mcp-date:
	python3 $(PYTHON_MCP_DATE_DIR)/server.py

build-java-mcp-date:
	mkdir -p $(JAVA_MCP_DATE_BUILD_DIR)
	javac -d $(JAVA_MCP_DATE_BUILD_DIR) $(JAVA_MCP_DATE_DIR)/src/*.java

run-java-mcp-date: build-java-mcp-date
	java -cp $(JAVA_MCP_DATE_BUILD_DIR) DateMcpServer

run-python-mcp-openweather:
	python3 $(PYTHON_MCP_OPENWEATHER_DIR)/server.py

run-python-mcp-openweather-fastmcp:
	cd mcp-client && $(UV_BIN) run python ../$(PYTHON_MCP_OPENWEATHER_FASTMCP_DIR)/server.py

build-java-mcp-openweather:
	mkdir -p $(JAVA_MCP_OPENWEATHER_BUILD_DIR)
	javac -d $(JAVA_MCP_OPENWEATHER_BUILD_DIR) $(JAVA_MCP_OPENWEATHER_DIR)/src/*.java

run-java-mcp-openweather: build-java-mcp-openweather
	java -cp $(JAVA_MCP_OPENWEATHER_BUILD_DIR) OpenWeatherMcpServer

setup-mcp-client:
	cd mcp-client && $(UV_BIN) sync

run-mcp-client:
	cd mcp-client && $(UV_BIN) run python main.py $(EXAMPLE) $(LANG) $(ARGS)

build-java-agent-example-ether-brain: build-java-api-hello build-java-mcp-hello
	mvn -q -f $(JAVA_AGENT_EXAMPLE_ETHER_BRAIN_DIR)/pom.xml compile

docker-build-python-api-hello:
	docker build -f $(CONTAINER_HELLO_DIR)/Dockerfile.python-api -t mcp-example/hello-python-api:latest .

docker-run-python-api-hello:
	mkdir -p logs
	docker run --rm -p 8080:8080 -v "$(CURDIR)/logs:/app/logs" mcp-example/hello-python-api:latest

docker-build-java-api-hello:
	docker build -f $(CONTAINER_HELLO_DIR)/Dockerfile.java-api -t mcp-example/hello-java-api:latest .

docker-run-java-api-hello:
	mkdir -p logs
	docker run --rm -p 8081:8081 -v "$(CURDIR)/logs:/app/logs" mcp-example/hello-java-api:latest

docker-up-hello:
	mkdir -p logs
	docker compose -f $(CONTAINER_HELLO_DIR)/compose.yaml up --build

docker-down-hello:
	docker compose -f $(CONTAINER_HELLO_DIR)/compose.yaml down
