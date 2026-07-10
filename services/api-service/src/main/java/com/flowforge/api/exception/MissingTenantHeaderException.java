package com.flowforge.api.exception;

public class MissingTenantHeaderException extends AppException {
    public MissingTenantHeaderException(String message) {
        super("MISSING_TENANT_HEADER", message, 400);
    }
}
