package dev.rafex.mcpexample.agents.java.etherbrain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HelloApiServerProcess implements AutoCloseable {
    private final Process process;

    private HelloApiServerProcess(Process process) {
        this.process = process;
    }

    public static HelloApiServerProcess start(Path repoRoot) throws IOException, InterruptedException {
        Path buildDir = repoRoot.resolve("backend/api-hello/java/build");
        if (!Files.isDirectory(buildDir)) {
            throw new IOException("No existe " + buildDir + ". Ejecuta primero make build-java-api-hello.");
        }

        Process process = new ProcessBuilder("java", "-cp", buildDir.toString(), "HelloApiServer")
            .directory(repoRoot.toFile())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        Thread.sleep(500L);
        return new HelloApiServerProcess(process);
    }

    @Override
    public void close() {
        process.destroy();
    }
}
