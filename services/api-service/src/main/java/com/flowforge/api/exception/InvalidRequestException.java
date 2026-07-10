package com.flowforge.api.exception;

public class InvalidRequestException extends AppException {
    public InvalidRequestException(String message) {
        super("VALIDATION_FAILED", message, 400);
    }
}
