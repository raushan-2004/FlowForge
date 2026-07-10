package com.flowforge.api.dto;

import java.util.UUID;

public class UserResponse {

    private UUID publicId;
    private String email;

    public UserResponse() {}

    public UserResponse(UUID publicId, String email) {
        this.publicId = publicId;
        this.email = email;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
