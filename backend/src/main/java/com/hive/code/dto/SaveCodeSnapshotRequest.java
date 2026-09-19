package com.hive.code.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveCodeSnapshotRequest(
        @NotNull
        @Size(max = 1_000_000)
        String content
) {
}
