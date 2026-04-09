PYTHON_API_HELLO_DIR := backend/api-hello/python
JAVA_API_HELLO_DIR := backend/api-hello/java
PYTHON_MCP_HELLO_DIR := mcp/hello/python
JAVA_MCP_HELLO_DIR := mcp/hello/java
JAVA_API_HELLO_BUILD_DIR := $(JAVA_API_HELLO_DIR)/build
JAVA_MCP_HELLO_BUILD_DIR := $(JAVA_MCP_HELLO_DIR)/build

.PHONY: run-python-api-hello build-java-api-hello run-java-api-hello run-python-mcp-hello build-java-mcp-hello run-java-mcp-hello

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
