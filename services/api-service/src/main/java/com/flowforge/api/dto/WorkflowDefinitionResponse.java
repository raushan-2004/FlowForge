package com.flowforge.api.dto;

import java.util.UUID;

public class WorkflowDefinitionResponse {
    private UUID publicId;
    private String name;
    private int version;
    private boolean active;
    private String definitionJson;

    public WorkflowDefinitionResponse() {}

    public WorkflowDefinitionResponse(UUID publicId, String name, int version, boolean active, String definitionJson) {
        this.publicId = publicId;
        this.name = name;
        this.version = version;
        this.active = active;
        this.definitionJson = definitionJson;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public String getDefinitionJson() {
        return definitionJson;
    }
}
