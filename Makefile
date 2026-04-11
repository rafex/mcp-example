PYTHON_API_HELLO_DIR := backend/api-hello/python
JAVA_API_HELLO_DIR := backend/api-hello/java
PYTHON_MCP_HELLO_DIR := mcp/hello/python
JAVA_MCP_HELLO_DIR := mcp/hello/java
JAVA_AGENT_EXAMPLE_ETHER_BRAIN_DIR := agents/java/agent-example-ether-brain
CONTAINER_HELLO_DIR := container/hello
JAVA_API_HELLO_BUILD_DIR := $(JAVA_API_HELLO_DIR)/build
JAVA_MCP_HELLO_BUILD_DIR := $(JAVA_MCP_HELLO_DIR)/build

.PHONY: run-python-api-hello build-java-api-hello run-java-api-hello run-python-mcp-hello build-java-mcp-hello run-java-mcp-hello build-java-agent-example-ether-brain docker-build-python-api-hello docker-run-python-api-hello docker-build-java-api-hello docker-run-java-api-hello docker-up-hello docker-down-hello

run-python-api-hello:
	python3 $(PYTHON_API_HELLO_DIR)/server.py

build-java-api-hello:
	mkdir -p $(JAVA_API_HELLO_BUILD_DIR)
	javac -d $(JAVA_API_HELLO_BUILD_DIR) $(JAVA_API_HELLO_DIR)/src/*.java

run-java-api-hello: build-java-api-hello
	java -cp $(JAVA_API_HELLO_BUILD_DIR) HelloApiServer

run-python-mcp-hello:
	python3 $(PYTHON_MCP_HELLO_DIR)/server.py

build-java-mcp-hello:
	mkdir -p $(JAVA_MCP_HELLO_BUILD_DIR)
	javac -d $(JAVA_MCP_HELLO_BUILD_DIR) $(JAVA_MCP_HELLO_DIR)/src/*.java

run-java-mcp-hello: build-java-mcp-hello
	java -cp $(JAVA_MCP_HELLO_BUILD_DIR) HelloMcpServer

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
