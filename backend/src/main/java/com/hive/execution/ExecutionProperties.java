package com.hive.execution;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hive.execution")
public record ExecutionProperties(
        String dockerImage,
        String dockerCommand,
        Duration timeout,
        String memoryLimit,
        String cpuLimit,
        int maxSourceLength,
        int maxOutputLength,
        int maxConcurrentExecutions,
        int perRoomRateLimitPerMinute
) {
    public ExecutionProperties {
        dockerImage = blankTo(dockerImage, "openjdk:21-slim");
        dockerCommand = blankTo(dockerCommand, "docker");
        timeout = timeout == null || timeout.isZero() || timeout.isNegative()
                ? Duration.ofSeconds(10) : timeout;
        memoryLimit = blankTo(memoryLimit, "128m");
        cpuLimit = blankTo(cpuLimit, "0.5");
        maxSourceLength = maxSourceLength <= 0 ? 100_000 : maxSourceLength;
        maxOutputLength = maxOutputLength <= 0 ? 64_000 : maxOutputLength;
        maxConcurrentExecutions = maxConcurrentExecutions <= 0 ? 2 : maxConcurrentExecutions;
        perRoomRateLimitPerMinute = perRoomRateLimitPerMinute <= 0 ? 10 : perRoomRateLimitPerMinute;
    }

    /** Docker flags that isolate the container: no network, read-only root, capped resources. */
    public List<String> isolationFlags() {
        return List.of(
                "--memory=" + memoryLimit,
                "--memory-swap=" + memoryLimit,
                "--cpus=" + cpuLimit,
                "--network=none",
                "--read-only",
                "--pids-limit=128",
                "--security-opt=no-new-privileges",
                "--cap-drop=ALL");
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
