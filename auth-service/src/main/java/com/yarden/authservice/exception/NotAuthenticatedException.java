package com.yarden.authservice.exception;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("Not authenticated");
    }
}
