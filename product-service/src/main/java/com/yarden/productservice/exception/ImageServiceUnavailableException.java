package com.yarden.productservice.exception;

public class ImageServiceUnavailableException extends RuntimeException {

    public ImageServiceUnavailableException(Throwable cause) {
        super("Image service is unavailable", cause);
    }
}
