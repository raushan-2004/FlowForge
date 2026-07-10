package com.flowforge.api.exception;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message, 401);
    }
}
