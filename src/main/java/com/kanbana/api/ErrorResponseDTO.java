package com.kanbana.api;

import java.time.Instant;

// Standard error body returned by GlobalExceptionHandler for all error responses.
// Every error — 400, 404, 500 — returns this same shape.
public record ErrorResponseDTO(int status, String message, Instant timestamp) {

    public static ErrorResponseDTO of(int status, String message) {
        return new ErrorResponseDTO(status, message, Instant.now());
    }
}
