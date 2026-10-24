package com.example.sis1.domain.exception;

/**
 * Raised when a request conflicts with the current state of a resource
 * (e.g. a duplicate unique field). Mapped to HTTP 409 Conflict.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
