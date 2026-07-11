package com.flowforge.api.controller;

import com.flowforge.api.dto.ExecutionAttemptResponse;
import com.flowforge.api.dto.ExecutionRequest;
import com.flowforge.api.dto.ExecutionResponse;
import com.flowforge.api.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> createExecution(@Valid @RequestBody ExecutionRequest request) {
        ExecutionResponse response = executionService.createExecution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> getExecutions(
            @RequestParam(value = "projectId", required = false) UUID projectId) {
        List<ExecutionResponse> response = executionService.getExecutions(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable("executionId") UUID executionId) {
        ExecutionResponse response = executionService.getExecution(executionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{executionId}/attempts")
    public ResponseEntity<List<ExecutionAttemptResponse>> getAttempts(@PathVariable("executionId") UUID executionId) {
        List<ExecutionAttemptResponse> response = executionService.getAttempts(executionId);
        return ResponseEntity.ok(response);
    }
}
