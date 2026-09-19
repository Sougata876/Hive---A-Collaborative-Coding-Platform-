package com.hive.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExecuteCodeRequest(
        @NotNull
        @Size(max = 100_000)
        String code
) {
}
