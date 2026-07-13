package com.flowforge.api.dto;

public class WorkflowDefinitionRequest {
    private String name;
    private String definitionJson;

    public WorkflowDefinitionRequest() {}

    public WorkflowDefinitionRequest(String name, String definitionJson) {
        this.name = name;
        this.definitionJson = definitionJson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinitionJson() {
        return definitionJson;
    }

    public void setDefinitionJson(String definitionJson) {
        this.definitionJson = definitionJson;
    }
}
