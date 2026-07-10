package com.flowforge.api.exception;

public class MembershipDeniedException extends AppException {
    public MembershipDeniedException(String message) {
        super("MEMBERSHIP_DENIED", message, 403);
    }
}
