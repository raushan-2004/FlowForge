package com.flowforge.api.controller;

import com.flowforge.api.dto.InternalExecutionFinalizeRequest;
import com.flowforge.api.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/executions")
public class InternalExecutionController {

    private final ExecutionService executionService;

    public InternalExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{executionPublicId}/finalize")
    public ResponseEntity<Void> finalizeExecution(
            @PathVariable("executionPublicId") UUID executionPublicId,
            @RequestBody InternalExecutionFinalizeRequest request) {
        executionService.finalizeExecution(executionPublicId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{executionPublicId}/schedule-retry")
    public ResponseEntity<Void> scheduleRetry(
            @PathVariable("executionPublicId") UUID executionPublicId,
            @RequestBody com.flowforge.api.dto.InternalExecutionRetryRequest request) {
        executionService.scheduleRetry(executionPublicId, request);
        return ResponseEntity.ok().build();
    }
}
