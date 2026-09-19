package com.hive.execution.dto;

import com.hive.execution.ExecutionResult;
import com.hive.execution.ExecutionStatus;

public record ExecutionResponse(
        ExecutionStatus status,
        String stdout,
        String stderr,
        Integer exitCode,
        long durationMs
) {
    public static ExecutionResponse from(ExecutionResult result) {
        return new ExecutionResponse(
                result.status(),
                result.stdout(),
                result.stderr(),
                result.exitCode(),
                result.durationMs());
    }
}
