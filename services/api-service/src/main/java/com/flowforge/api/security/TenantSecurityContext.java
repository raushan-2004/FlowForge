package com.flowforge.api.security;

import com.flowforge.api.model.TenantRole;
import java.util.UUID;

public class TenantSecurityContext {

    private final IdentityType identityType;
    private final UUID userPublicId;
    private final UUID projectPublicId;
    private final UUID tenantPublicId;
    private final Long tenantInternalId;
    private final TenantRole role;

    /**
     * Constructor for Human User Identity context.
     */
    public TenantSecurityContext(UUID userPublicId, UUID tenantPublicId, Long tenantInternalId, TenantRole role) {
        if (userPublicId == null) throw new IllegalArgumentException("userPublicId cannot be null");
        if (tenantPublicId == null) throw new IllegalArgumentException("tenantPublicId cannot be null");
        if (tenantInternalId == null) throw new IllegalArgumentException("tenantInternalId cannot be null");
        if (role == null) throw new IllegalArgumentException("role cannot be null");

        this.identityType = IdentityType.HUMAN;
        this.userPublicId = userPublicId;
        this.projectPublicId = null;
        this.tenantPublicId = tenantPublicId;
        this.tenantInternalId = tenantInternalId;
        this.role = role;
    }

    /**
     * Constructor for Automation Client Identity context.
     */
    public TenantSecurityContext(UUID projectPublicId, UUID tenantPublicId, Long tenantInternalId) {
        if (projectPublicId == null) throw new IllegalArgumentException("projectPublicId cannot be null");
        if (tenantPublicId == null) throw new IllegalArgumentException("tenantPublicId cannot be null");
        if (tenantInternalId == null) throw new IllegalArgumentException("tenantInternalId cannot be null");

        this.identityType = IdentityType.AUTOMATION;
        this.userPublicId = null;
        this.projectPublicId = projectPublicId;
        this.tenantPublicId = tenantPublicId;
        this.tenantInternalId = tenantInternalId;
        this.role = null;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    public boolean isHuman() {
        return identityType == IdentityType.HUMAN;
    }

    public boolean isAutomation() {
        return identityType == IdentityType.AUTOMATION;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public UUID getProjectPublicId() {
        return projectPublicId;
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
