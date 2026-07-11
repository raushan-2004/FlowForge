package com.flowforge.api.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, 404);
    }
}
