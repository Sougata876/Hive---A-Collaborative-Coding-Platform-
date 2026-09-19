package com.hive.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path, Instant timestamp) {
        return new ApiError(timestamp, status, error, message, path, Map.of());
    }
}
