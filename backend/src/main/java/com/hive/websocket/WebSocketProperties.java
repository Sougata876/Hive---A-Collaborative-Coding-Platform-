package com.hive.websocket;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hive.websocket")
public record WebSocketProperties(List<String> allowedOrigins) {

    public WebSocketProperties {
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:5173")
                : List.copyOf(allowedOrigins);
    }
}
