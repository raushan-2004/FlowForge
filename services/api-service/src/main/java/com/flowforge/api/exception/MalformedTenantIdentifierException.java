package com.flowforge.api.exception;

public class MalformedTenantIdentifierException extends AppException {
    public java.util.UUID tenantPublicId;
    public MalformedTenantIdentifierException(String message) {
        super("MALFORMED_TENANT_IDENTIFIER", message, 400);
    }
}
