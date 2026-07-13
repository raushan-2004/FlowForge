package com.flowforge.api.controller;

import com.flowforge.api.dto.*;
import com.flowforge.api.model.WorkflowDefinition;
import com.flowforge.api.model.WorkflowRun;
import com.flowforge.api.service.WorkflowEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class WorkflowController {

    private final WorkflowEngineService workflowEngineService;

    public WorkflowController(WorkflowEngineService workflowEngineService) {
        this.workflowEngineService = workflowEngineService;
    }

    @PostMapping("/api/v1/projects/{projectPublicId}/workflows")
    public ResponseEntity<WorkflowDefinitionResponse> createWorkflow(
            @PathVariable("projectPublicId") UUID projectPublicId,
            @RequestBody WorkflowDefinitionRequest request) {
        WorkflowDefinition def = workflowEngineService.createDefinition(projectPublicId, request.getName(), request.getDefinitionJson());
        WorkflowDefinitionResponse response = new WorkflowDefinitionResponse(
                def.getPublicId(),
                def.getName(),
                def.getVersion(),
                def.isActive(),
                def.getDefinitionJson()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/workflows/{definitionPublicId}/runs")
    public ResponseEntity<WorkflowRunResponse> startWorkflowRun(
            @PathVariable("definitionPublicId") UUID definitionPublicId) {
        WorkflowRun run = workflowEngineService.startWorkflowRun(definitionPublicId);
        WorkflowRunResponse response = new WorkflowRunResponse(
                run.getPublicId(),
                run.getWorkflowDefinition().getPublicId(),
                run.getStatus().name(),
                run.getStartedAt(),
                run.getFinishedAt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/internal/v1/workflows/process-node-completion")
    public ResponseEntity<Void> processNodeCompletion(
            @RequestBody InternalWorkflowNodeCompleteRequest request) {
        workflowEngineService.processNodeCompletion(
                request.getExecutionPublicId(),
                request.getFinalStatus(),
                request.getHttpStatus(),
                request.getNetworkError()
        );
        return ResponseEntity.ok().build();
    }
}
