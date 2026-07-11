package com.flowforge.api.exception;

public class DuplicateTenantNameException extends AppException {
    public DuplicateTenantNameException(String message) {
        super("DUPLICATE_TENANT_NAME", message, 409);
    }
}
