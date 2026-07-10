package com.flowforge.api.exception;

public class DuplicateEmailException extends AppException {
    public DuplicateEmailException(String message) {
        super("DUPLICATE_EMAIL", message, 409);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super("DUPLICATE_EMAIL", message, 409, cause);
    }
}
