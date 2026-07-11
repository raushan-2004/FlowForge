package com.flowforge.api.controller;

import com.flowforge.api.dto.ProjectRequest;
import com.flowforge.api.dto.ProjectResponse;
import com.flowforge.api.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        List<ProjectResponse> response = projectService.getProjects();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable("projectId") UUID projectId) {
        ProjectResponse response = projectService.getProject(projectId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable("projectId") UUID projectId,
            @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable("projectId") UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
