package com.flowforge.api.dto;

import com.flowforge.api.model.TenantRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class MembershipRequest {

    private UUID userPublicId;

    @NotNull(message = "Role is required")
    private TenantRole role;

    public MembershipRequest() {}

    public MembershipRequest(UUID userPublicId, TenantRole role) {
        this.userPublicId = userPublicId;
        this.role = role;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public void setUserPublicId(UUID userPublicId) {
        this.userPublicId = userPublicId;
    }

    public TenantRole getRole() {
        return role;
    }

    public void setRole(TenantRole role) {
        this.role = role;
    }
}
