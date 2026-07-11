package com.flowforge.api.controller;

import com.flowforge.api.dto.JobRequest;
import com.flowforge.api.dto.JobResponse;
import com.flowforge.api.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
            @RequestParam(value = "projectId", required = false) UUID projectId) {
        List<JobResponse> response = jobService.getJobs(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable("jobId") UUID jobId) {
        JobResponse response = jobService.getJob(jobId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{jobId}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable("jobId") UUID jobId,
            @Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.updateJob(jobId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable("jobId") UUID jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }
}
