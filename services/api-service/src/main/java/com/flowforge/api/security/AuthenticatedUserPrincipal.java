package com.flowforge.api.security;

import java.security.Principal;
import java.util.UUID;

public class AuthenticatedUserPrincipal implements Principal {

    private final UUID userPublicId;

    public AuthenticatedUserPrincipal(UUID userPublicId) {
        if (userPublicId == null) {
            throw new IllegalArgumentException("userPublicId cannot be null");
        }
        this.userPublicId = userPublicId;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    @Override
    public String getName() {
        return userPublicId.toString();
    }
}
