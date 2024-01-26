package com.si.googleads.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse<T> {
    private boolean success;
    private String message;
    private T error;

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
    }

    public ErrorResponse(String message, T error) {
        this.success = false;
        this.message = message;
        this.error = error;
    }
}
