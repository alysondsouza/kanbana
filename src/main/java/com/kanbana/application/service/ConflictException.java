package com.kanbana.application.service;

// Thrown when a uniqueness constraint would be violated — e.g. duplicate username or email.
// Caught by GlobalExceptionHandler and mapped to HTTP 409 Conflict.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
