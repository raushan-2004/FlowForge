package com.flowforge.api.exception;

public class SuspendedAccountException extends AppException {
    public SuspendedAccountException(String message) {
        super("SUSPENDED_ACCOUNT", message, 401);
    }
}
