package com.hive.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExecutionPropertiesTests {

    @Test
    void appliesSafeDefaultsWhenUnset() {
        var properties = new ExecutionProperties(null, null, null, null, null, 0, 0, 0, 0);

        assertThat(properties.dockerImage()).isEqualTo("openjdk:21-slim");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.memoryLimit()).isEqualTo("128m");
        assertThat(properties.cpuLimit()).isEqualTo("0.5");
        assertThat(properties.maxConcurrentExecutions()).isEqualTo(2);
        assertThat(properties.perRoomRateLimitPerMinute()).isEqualTo(10);
    }

    @Test
    void isolationFlagsEnforceTheSandboxBoundary() {
        var properties = new ExecutionProperties(
                "openjdk:21-slim", "docker", Duration.ofSeconds(10), "128m", "0.5", 100, 100, 2, 10);

        assertThat(properties.isolationFlags())
                .contains("--memory=128m", "--cpus=0.5", "--network=none", "--read-only")
                .contains("--cap-drop=ALL", "--security-opt=no-new-privileges")
                // Without capping swap, a container could exceed its memory limit on disk.
                .contains("--memory-swap=128m");
    }

    @Test
    void rejectsNonPositiveTimeoutInFavourOfTheDefault() {
        var zero = new ExecutionProperties(null, null, Duration.ZERO, null, null, 0, 0, 0, 0);
        var negative = new ExecutionProperties(
                null, null, Duration.ofSeconds(-5), null, null, 0, 0, 0, 0);

        assertThat(zero.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(negative.timeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
