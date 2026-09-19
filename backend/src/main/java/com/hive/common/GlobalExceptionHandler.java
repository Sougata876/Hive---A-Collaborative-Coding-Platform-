package com.hive.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiError body = new ApiError(clock.instant(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Request validation failed", request.getRequestURI(), fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiError> handleBadRequest(InvalidRequestException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(com.hive.execution.RateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimit(com.hive.execution.RateLimitExceededException exception,
                                            HttpServletRequest request) {
        return response(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({BadCredentialsException.class, InvalidRefreshTokenException.class})
    ResponseEntity<ApiError> handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI(), clock.instant()));
    }
}
