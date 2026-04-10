package dev.rafex.mcpexample.agents.java.etherbrain;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HelloMcpClient implements AutoCloseable {
    private final Process process;
    private final BufferedInputStream input;
    private final OutputStream output;
    private long nextId;

    private HelloMcpClient(Process process) throws IOException {
        this.process = process;
        this.input = new BufferedInputStream(process.getInputStream());
        this.output = process.getOutputStream();
        this.nextId = 1L;
        initialize();
    }

    public static HelloMcpClient start(Path repoRoot) throws IOException {
        Path buildDir = repoRoot.resolve("mcp/hello/java/build");
        if (!Files.isDirectory(buildDir)) {
            throw new IOException("No existe " + buildDir + ". Ejecuta primero make build-java-mcp-hello.");
        }

        Process process = new ProcessBuilder("java", "-cp", buildDir.toString(), "HelloMcpServer")
            .directory(repoRoot.toFile())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        return new HelloMcpClient(process);
    }

    public synchronized String callHello(String name, String lang, String ip) throws IOException {
        long id = nextId++;
        String request = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"method\":\"tools/call\","
            + "\"params\":{"
            + "\"name\":\"say_hello\","
            + "\"arguments\":" + buildArguments(name, lang, ip)
            + "}"
            + "}";
        writeMessage(request);
        String response = readMessage();
        String structuredContent = extractObject(response, "\"structuredContent\":");
        if (structuredContent == null) {
            throw new IOException("Respuesta MCP sin structuredContent: " + response);
        }
        return structuredContent;
    }

    public synchronized String getHelloLanguages() throws IOException {
        long id = nextId++;
        String request = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"method\":\"tools/call\","
            + "\"params\":{"
            + "\"name\":\"get_hello_languages\","
            + "\"arguments\":{}"
            + "}"
            + "}";
        writeMessage(request);
        String response = readMessage();
        String structuredContent = extractObject(response, "\"structuredContent\":");
        if (structuredContent == null) {
            throw new IOException("Respuesta MCP sin structuredContent: " + response);
        }
        return structuredContent;
    }

    private void initialize() throws IOException {
        long id = nextId++;
        String initializeRequest = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"method\":\"initialize\","
            + "\"params\":{"
            + "\"protocolVersion\":\"2024-11-05\","
            + "\"capabilities\":{},"
            + "\"clientInfo\":{\"name\":\"agent-example-ether-brain\",\"version\":\"0.1.0\"}"
            + "}"
            + "}";
        writeMessage(initializeRequest);
        readMessage();

        String initializedNotification = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"method\":\"notifications/initialized\","
            + "\"params\":{}"
            + "}";
        writeMessage(initializedNotification);
    }

    private String buildArguments(String name, String lang, String ip) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        first = appendJsonField(builder, "name", name, first);
        first = appendJsonField(builder, "lang", lang, first);
        appendJsonField(builder, "ip", ip, first);
        builder.append("}");
        return builder.toString();
    }

    private boolean appendJsonField(StringBuilder builder, String key, String value, boolean first) {
        if (value == null || value.isBlank()) {
            return first;
        }
        if (!first) {
            builder.append(",");
        }
        builder.append("\"").append(escapeJson(key)).append("\":");
        builder.append("\"").append(escapeJson(value)).append("\"");
        return false;
    }

    private void writeMessage(String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + body.length + "\r\n\r\n";
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }

    private String readMessage() throws IOException {
        Integer contentLength = null;
        while (true) {
            String line = readHeaderLine();
            if (line == null) {
                throw new EOFException("EOF mientras se leian cabeceras MCP.");
            }
            if (line.isEmpty()) {
                break;
            }
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }

        if (contentLength == null) {
            throw new IOException("Mensaje MCP sin Content-Length.");
        }

        byte[] body = input.readNBytes(contentLength);
        if (body.length != contentLength) {
            throw new EOFException("EOF mientras se leia el body MCP.");
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String readHeaderLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int next = input.read();
            if (next == -1) {
                if (builder.isEmpty()) {
                    return null;
                }
                throw new EOFException("EOF inesperado en header MCP.");
            }
            if (next == '\r') {
                int following = input.read();
                if (following == '\n') {
                    return builder.toString();
                }
                if (following != -1) {
                    builder.append((char) next);
                    builder.append((char) following);
                }
                continue;
            }
            if (next == '\n') {
                return builder.toString();
            }
            builder.append((char) next);
        }
    }

    private String extractObject(String json, String marker) {
        int markerIndex = json.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        int start = json.indexOf('{', markerIndex + marker.length());
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        for (int i = start; i < json.length(); i++) {
            char current = json.charAt(i);
            boolean escaped = i > start && json.charAt(i - 1) == '\\';
            if (current == '"' && !escaped) {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (char current : value.toCharArray()) {
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }

    @Override
    public void close() {
        process.destroy();
    }
}
