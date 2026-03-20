package com.kanbana.application.service;

// Thrown when a requested resource does not exist in the database.
// Caught by GlobalExceptionHandler and mapped to HTTP 404.
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
