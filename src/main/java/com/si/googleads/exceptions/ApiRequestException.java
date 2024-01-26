package com.si.googleads.exceptions;

public class ApiRequestException extends RuntimeException {
    public ApiRequestException() {}

    public ApiRequestException(String message) {
        super(message);
    }
}
