package com.flowforge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    private String name;

    private String status;

    public TenantRequest() {}

    public TenantRequest(String name) {
        this.name = name;
    }

    public TenantRequest(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
