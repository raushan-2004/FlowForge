package com.flowforge.api.security;

import com.flowforge.api.model.TenantRole;
import java.util.UUID;

public class TenantSecurityContext {

    private final UUID userPublicId;
    private final UUID tenantPublicId;
    private final Long tenantInternalId;
    private final TenantRole role;

    public TenantSecurityContext(UUID userPublicId, UUID tenantPublicId, Long tenantInternalId, TenantRole role) {
        if (userPublicId == null) throw new IllegalArgumentException("userPublicId cannot be null");
        if (tenantPublicId == null) throw new IllegalArgumentException("tenantPublicId cannot be null");
        if (tenantInternalId == null) throw new IllegalArgumentException("tenantInternalId cannot be null");
        if (role == null) throw new IllegalArgumentException("role cannot be null");

        this.userPublicId = userPublicId;
        this.tenantPublicId = tenantPublicId;
        this.tenantInternalId = tenantInternalId;
        this.role = role;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public UUID getTenantPublicId() {
        return tenantPublicId;
    }

    public Long getTenantInternalId() {
        return tenantInternalId;
    }

    public TenantRole getRole() {
        return role;
    }
}
