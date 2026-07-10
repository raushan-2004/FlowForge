package com.flowforge.api.exception;

public class TenantNotFoundException extends AppException {
    public TenantNotFoundException(String message) {
        super("TENANT_NOT_FOUND", message, 403);
    }
}
