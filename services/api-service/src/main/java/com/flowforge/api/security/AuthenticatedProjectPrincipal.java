package com.flowforge.api.security;

import java.security.Principal;
import java.util.UUID;

public class AuthenticatedProjectPrincipal implements Principal {

    private final UUID projectId;
    private final UUID tenantId;

    public AuthenticatedProjectPrincipal(UUID projectId, UUID tenantId) {
        if (projectId == null) throw new IllegalArgumentException("projectId cannot be null");
        if (tenantId == null) throw new IllegalArgumentException("tenantId cannot be null");

        this.projectId = projectId;
        this.tenantId = tenantId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public String getName() {
        return "project_" + projectId.toString();
    }
}
