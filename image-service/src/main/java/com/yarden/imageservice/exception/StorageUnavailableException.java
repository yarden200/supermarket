package com.yarden.imageservice.exception;

public class StorageUnavailableException extends RuntimeException {
  public StorageUnavailableException(Throwable cause) {
    super("Image storage is unavailable", cause);
  }
}