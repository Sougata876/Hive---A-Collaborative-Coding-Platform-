package com.hive.execution;

import com.hive.room.ProgrammingLanguage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs Java sources inside a single-use Docker container.
 *
 * <p>The container has no network, a read-only root filesystem with a small writable {@code /tmp},
 * capped memory and CPU, and a hard wall-clock limit. Submitted code never executes in the server
 * JVM.
 */
@Component
public class DockerJavaExecutor implements LanguageExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerJavaExecutor.class);
    private static final String WORKDIR = "/workspace";

    private final ExecutionProperties properties;

    public DockerJavaExecutor(ExecutionProperties properties) {
        this.properties = properties;
    }

    @Override
    public ProgrammingLanguage language() {
        return ProgrammingLanguage.JAVA;
    }

    @Override
    public ExecutionResult execute(String sourceCode) {
        Path workspace;
        try {
            workspace = Files.createTempDirectory("hive-exec-");
            Files.writeString(workspace.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.error("Could not stage the execution workspace", exception);
            return ExecutionResult.error("Could not prepare the execution sandbox.");
        }

        try {
            return runContainer(workspace);
        } finally {
            deleteRecursively(workspace);
        }
    }

    private ExecutionResult runContainer(Path workspace) {
        String containerName = "hive-exec-" + UUID.randomUUID();
        Duration timeout = properties.timeout();

        List<String> command = new ArrayList<>();
        command.add(properties.dockerCommand());
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add(containerName);
        command.addAll(properties.isolationFlags());
        // Scratch space for javac output; the rest of the filesystem stays read-only.
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=32m");
        command.add("--volume");
        command.add(workspace.toAbsolutePath() + ":" + WORKDIR + ":ro");
        command.add("--workdir");
        command.add(WORKDIR);
        command.add(properties.dockerImage());
        command.add("sh");
        command.add("-c");
        command.add("javac -d /tmp/classes " + WORKDIR + "/Main.java && java -cp /tmp/classes Main");

        Process process;
        long startedAt = System.nanoTime();
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException exception) {
            log.error("Could not start the execution container", exception);
            return ExecutionResult.error(
                    "The execution sandbox is unavailable. Verify that Docker is installed and running.");
        }

        try {
            // Close stdin so a program waiting on input fails fast instead of burning its timeout.
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // Nothing to do: the process will still be bounded by the timeout below.
        }

        StreamCollector stdout = StreamCollector.start(process.getInputStream(), properties.maxOutputLength());
        StreamCollector stderr = StreamCollector.start(process.getErrorStream(), properties.maxOutputLength());

        try {
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                // Kill the container itself; killing the client process would leave it running.
                killContainer(containerName);
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                return ExecutionResult.timedOut(stdout.join(), stderr.join(), timeout);
            }

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            int exitCode = process.exitValue();
            ExecutionStatus status = exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
            return ExecutionResult.of(status, stdout.join(), stderr.join(), exitCode, elapsed);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            killContainer(containerName);
            process.destroyForcibly();
            return ExecutionResult.error("Execution was interrupted.");
        }
    }

    private void killContainer(String containerName) {
        try {
            new ProcessBuilder(properties.dockerCommand(), "kill", containerName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(10, TimeUnit.SECONDS);
        } catch (IOException exception) {
            log.warn("Could not kill container {}", containerName, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void deleteRecursively(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (IOException | UncheckedIOException exception) {
            log.warn("Could not clean up the execution workspace {}", root, exception);
        }
    }
}
