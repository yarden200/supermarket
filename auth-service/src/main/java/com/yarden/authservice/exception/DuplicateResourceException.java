package com.yarden.authservice.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resourceName, String field, String value) {
        super(resourceName + " with " + field + " '" + value + "' already exists");
    }
}
