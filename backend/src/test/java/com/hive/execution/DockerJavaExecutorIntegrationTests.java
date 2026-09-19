package com.hive.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Exercises the real Docker sandbox. Skipped when Docker is unavailable so the suite stays runnable
 * on machines without it — but when Docker is present these are the tests that prove the resource
 * limits actually hold.
 */
@EnabledIf("dockerAvailable")
class DockerJavaExecutorIntegrationTests {

    static boolean dockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private DockerJavaExecutor executor(Duration timeout) {
        return new DockerJavaExecutor(new ExecutionProperties(
                "openjdk:21-slim", "docker", timeout, "128m", "0.5", 100_000, 64_000, 2, 10));
    }

    @Test
    void runsAJavaProgramAndCapturesStdout() {
        var result = executor(Duration.ofSeconds(60)).execute("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello from Hive!");
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("Hello from Hive!");
        assertThat(result.exitCode()).isZero();
    }

    @Test
    void reportsCompilationFailureWithoutRunningTheProgram() {
        var result = executor(Duration.ofSeconds(60)).execute("public class Main { oops }");

        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.stderr()).isNotBlank();
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    void killsAProgramThatExceedsTheTimeout() {
        var result = executor(Duration.ofSeconds(15)).execute("""
                public class Main {
                    public static void main(String[] args) throws Exception {
                        while (true) { Thread.sleep(1000); }
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(ExecutionStatus.TIMEOUT);
        assertThat(result.stderr()).contains("timed out");
    }

    @Test
    void networkAccessIsBlockedInsideTheSandbox() {
        var result = executor(Duration.ofSeconds(60)).execute("""
                import java.net.Socket;

                public class Main {
                    public static void main(String[] args) {
                        try (Socket socket = new Socket("example.com", 80)) {
                            System.out.println("NETWORK REACHED");
                        } catch (Exception exception) {
                            System.out.println("network blocked");
                        }
                    }
                }
                """);

        assertThat(result.stdout()).contains("network blocked");
        assertThat(result.stdout()).doesNotContain("NETWORK REACHED");
    }

    @Test
    void theRootFilesystemIsReadOnly() {
        var result = executor(Duration.ofSeconds(60)).execute("""
                import java.nio.file.Files;
                import java.nio.file.Path;

                public class Main {
                    public static void main(String[] args) {
                        try {
                            Files.writeString(Path.of("/escaped.txt"), "nope");
                            System.out.println("WROTE TO ROOT");
                        } catch (Exception exception) {
                            System.out.println("root is read-only");
                        }
                    }
                }
                """);

        assertThat(result.stdout()).contains("root is read-only");
        assertThat(result.stdout()).doesNotContain("WROTE TO ROOT");
    }

    @Test
    void memoryLimitStopsARunawayAllocation() {
        var result = executor(Duration.ofSeconds(60)).execute("""
                import java.util.ArrayList;
                import java.util.List;

                public class Main {
                    public static void main(String[] args) {
                        List<byte[]> held = new ArrayList<>();
                        try {
                            while (true) held.add(new byte[8 * 1024 * 1024]);
                        } catch (OutOfMemoryError error) {
                            System.out.println("memory capped");
                        }
                    }
                }
                """);

        // Either the JVM reports OOM or the container is killed outright; both mean the cap held.
        assertThat(result.status()).isNotEqualTo(ExecutionStatus.SUCCESS)
                .satisfiesAnyOf(
                        status -> assertThat(result.stdout()).contains("memory capped"),
                        status -> assertThat(result.status()).isIn(
                                ExecutionStatus.FAILED, ExecutionStatus.TIMEOUT));
    }
}
