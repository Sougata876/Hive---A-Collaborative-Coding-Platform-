package com.hive.websocket;

import org.springframework.security.access.AccessDeniedException;

public class WebSocketAccessDeniedException extends AccessDeniedException {

    public WebSocketAccessDeniedException(String message) {
        super(message);
    }
}
