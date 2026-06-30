package com.example.parkinglot.sdk;

public class ApiClientException extends RuntimeException {

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiClientException(String message) {
        super(message);
    }
}

