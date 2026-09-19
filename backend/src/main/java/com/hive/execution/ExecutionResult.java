package com.hive.execution;

import java.time.Duration;

public record ExecutionResult(
        ExecutionStatus status,
        String stdout,
        String stderr,
        Integer exitCode,
        long durationMs
) {
    public static ExecutionResult of(ExecutionStatus status, String stdout, String stderr,
                                     Integer exitCode, Duration duration) {
        return new ExecutionResult(status, stdout, stderr, exitCode, duration.toMillis());
    }

    public static ExecutionResult timedOut(String stdout, String stderr, Duration limit) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, stdout,
                stderr + "\nExecution timed out after " + limit.toSeconds() + "s and was terminated.",
                null, limit.toMillis());
    }

    public static ExecutionResult error(String message) {
        return new ExecutionResult(ExecutionStatus.ERROR, "", message, null, 0);
    }
}
