package com.flowforge.api.dto;

import com.flowforge.api.model.TenantRole;
import java.time.Instant;
import java.util.UUID;

public class MembershipResponse {

    private UUID userPublicId;
    private String email;
    private TenantRole role;
    private Instant createdAt;

    public MembershipResponse() {}

    public MembershipResponse(UUID userPublicId, String email, TenantRole role, Instant createdAt) {
        this.userPublicId = userPublicId;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public void setUserPublicId(UUID userPublicId) {
        this.userPublicId = userPublicId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TenantRole getRole() {
        return role;
    }

    public void setRole(TenantRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
