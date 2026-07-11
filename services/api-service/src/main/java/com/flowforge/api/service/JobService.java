package com.flowforge.api.service;

import com.flowforge.api.dto.JobRequest;
import com.flowforge.api.dto.JobResponse;
import com.flowforge.api.exception.InvalidRequestException;
import com.flowforge.api.exception.MembershipDeniedException;
import com.flowforge.api.exception.ResourceNotFoundException;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.JobRepository;
import com.flowforge.api.repository.ProjectRepository;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final TenantAuthorizationService authorizationService;
    private final PublicIdGenerator publicIdGenerator;
    private final Clock clock;

    public JobService(
            JobRepository jobRepository,
            ProjectRepository projectRepository,
            TenantAuthorizationService authorizationService,
            PublicIdGenerator publicIdGenerator,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.publicIdGenerator = publicIdGenerator;
        this.clock = clock;
    }

    private TenantSecurityContext getActiveTenantContext() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            throw new MembershipDeniedException("Active tenant context is required");
        }
        return context;
    }

    private UUID getActorPublicId(TenantSecurityContext context) {
        if (context.isHuman()) {
            return context.getUserPublicId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000000"); // System user
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canCreateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can create jobs");
        }

        validateJobRequest(request);

        if (request.getProjectPublicId() == null) {
            throw new InvalidRequestException("Project public ID is required");
        }

        Project project = projectRepository.findByPublicIdAndTenantPublicId(request.getProjectPublicId(), context.getTenantPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (jobRepository.findByProjectIdAndNameIgnoreCase(project.getId(), request.getName()).isPresent()) {
            throw new InvalidRequestException("Job name is already taken within this project");
        }

        int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 30;
        boolean enabled = request.getEnabled() != null ? request.getEnabled() : true;
        JobStatus status = request.getScheduleType() != null && JobScheduleType.valueOf(request.getScheduleType().trim().toUpperCase()) == JobScheduleType.CRON 
                ? JobStatus.ACTIVE : JobStatus.DRAFT;

        JobHttpMethod httpMethod = JobHttpMethod.valueOf(request.getHttpMethod().trim().toUpperCase());
        JobScheduleType scheduleType = JobScheduleType.valueOf(request.getScheduleType().trim().toUpperCase());

        // Target URL normalization
        String normalizedUrl = request.getTargetUrl().trim();
        try {
            normalizedUrl = new java.net.URI(normalizedUrl).normalize().toString();
        } catch (Exception ignored) {}

        Job job = new Job(
                publicIdGenerator.generate(),
                project,
                request.getName(),
                request.getDescription(),
                enabled,
                httpMethod,
                normalizedUrl,
                request.getRequestHeaders(),
                request.getRequestBody(),
                timeout,
                request.getRetryMaxAttempts(),
                request.getRetryStrategy(),
                request.getRetryBaseDelaySeconds(),
                scheduleType,
                request.getCronExpression(),
                status,
                getActorPublicId(context),
                clock.instant()
        );

        job = jobRepository.save(job);
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(UUID projectId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (projectId != null) {
            if (context.isAutomation() && !context.getProjectPublicId().equals(projectId)) {
                throw new ResourceNotFoundException("Project not found");
            }
            return jobRepository.findAllByProjectPublicIdAndTenantPublicId(projectId, context.getTenantPublicId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (context.isAutomation()) {
            return jobRepository.findAllByProjectPublicIdAndTenantPublicId(context.getProjectPublicId(), context.getTenantPublicId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return jobRepository.findAllByTenantPublicId(context.getTenantPublicId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        TenantSecurityContext context = getActiveTenantContext();

        Job job;
        if (context.isAutomation()) {
            job = jobRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(jobId, context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        } else {
            job = jobRepository.findByPublicIdAndTenantPublicId(jobId, context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        }

        return mapToResponse(job);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, JobRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canUpdateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can update jobs");
        }

        Job job;
        if (context.isAutomation()) {
            job = jobRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(jobId, context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        } else {
            job = jobRepository.findByPublicIdAndTenantPublicId(jobId, context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        }

        validateJobRequest(request);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(job.getName())) {
                if (jobRepository.findByProjectIdAndNameIgnoreCase(job.getProject().getId(), newName).isPresent()) {
                    throw new InvalidRequestException("Job name is already taken within this project");
                }
            }
        }

        int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : job.getTimeoutSeconds();
        boolean enabled = request.getEnabled() != null ? request.getEnabled() : job.isEnabled();
        
        JobHttpMethod httpMethod = request.getHttpMethod() != null 
                ? JobHttpMethod.valueOf(request.getHttpMethod().trim().toUpperCase()) : job.getHttpMethod();
        
        JobScheduleType scheduleType = request.getScheduleType() != null 
                ? JobScheduleType.valueOf(request.getScheduleType().trim().toUpperCase()) : job.getScheduleType();

        JobStatus status = job.getStatus();
        if (request.getScheduleType() != null) {
            status = scheduleType == JobScheduleType.CRON ? JobStatus.ACTIVE : JobStatus.DRAFT;
        }

        String normalizedUrl = job.getTargetUrl();
        if (request.getTargetUrl() != null) {
            normalizedUrl = request.getTargetUrl().trim();
            try {
                normalizedUrl = new java.net.URI(normalizedUrl).normalize().toString();
            } catch (Exception ignored) {}
        }

        job.updateDetails(
                request.getName(),
                request.getDescription(),
                enabled,
                httpMethod,
                normalizedUrl,
                request.getRequestHeaders() != null ? request.getRequestHeaders() : job.getRequestHeaders(),
                request.getRequestBody(),
                timeout,
                request.getRetryMaxAttempts(),
                request.getRetryStrategy(),
                request.getRetryBaseDelaySeconds(),
                scheduleType,
                request.getCronExpression(),
                status,
                getActorPublicId(context),
                clock.instant()
        );

        job = jobRepository.save(job);
        return mapToResponse(job);
    }

    @Transactional
    public void deleteJob(UUID jobId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canArchiveProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can archive jobs");
        }

        Job job;
        if (context.isAutomation()) {
            job = jobRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(jobId, context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        } else {
            job = jobRepository.findByPublicIdAndTenantPublicId(jobId, context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        }

        job.archive(getActorPublicId(context), clock.instant());
        jobRepository.save(job);
    }

    private void validateJobRequest(JobRequest request) {
        if (request.getName() != null && request.getName().trim().isEmpty()) {
            throw new InvalidRequestException("Job name cannot be blank");
        }

        // 1. HTTP Method validation
        if (request.getHttpMethod() != null) {
            try {
                JobHttpMethod.valueOf(request.getHttpMethod().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Unsupported HTTP method: " + request.getHttpMethod());
            }
        }

        // 2. URL validation
        if (request.getTargetUrl() != null) {
            String urlStr = request.getTargetUrl().trim();
            if (urlStr.length() > 2048) {
                throw new InvalidRequestException("Target URL exceeds maximum length of 2048 characters");
            }
            try {
                java.net.URI uri = new java.net.URI(urlStr);
                if (!uri.isAbsolute()) {
                    throw new InvalidRequestException("Target URL must be an absolute URI");
                }
                String scheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                    throw new InvalidRequestException("Unsupported URL scheme: " + scheme);
                }
            } catch (java.net.URISyntaxException e) {
                throw new InvalidRequestException("Invalid Target URL format");
            }
        }

        // 3. Headers validation (case-insensitive duplicate check)
        if (request.getRequestHeaders() != null) {
            java.util.Set<String> lowerKeys = new java.util.HashSet<>();
            for (String key : request.getRequestHeaders().keySet()) {
                if (key != null) {
                    String normKey = key.trim().toLowerCase();
                    if (!lowerKeys.add(normKey)) {
                        throw new InvalidRequestException("Duplicate header name: " + key);
                    }
                }
            }
        }

        // 4. Timeout bounds validation
        if (request.getTimeoutSeconds() != null) {
            int timeout = request.getTimeoutSeconds();
            if (timeout < 1 || timeout > 300) {
                throw new InvalidRequestException("Timeout must be between 1 and 300 seconds");
            }
        }

        // 5. Retry metadata validation
        if (request.getRetryMaxAttempts() != null && request.getRetryMaxAttempts() < 1) {
            throw new InvalidRequestException("Retry attempts must be at least 1");
        }
        if (request.getRetryBaseDelaySeconds() != null && request.getRetryBaseDelaySeconds() < 1) {
            throw new InvalidRequestException("Retry base delay must be at least 1 second");
        }

        // 6. Schedule validation
        if (request.getScheduleType() != null) {
            JobScheduleType scheduleType;
            try {
                scheduleType = JobScheduleType.valueOf(request.getScheduleType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Invalid schedule type: " + request.getScheduleType());
            }

            if (scheduleType == JobScheduleType.CRON) {
                if (request.getCronExpression() == null || request.getCronExpression().trim().isEmpty()) {
                    throw new InvalidRequestException("Cron expression is required for CRON schedule type");
                }
                if (!org.springframework.scheduling.support.CronExpression.isValidExpression(request.getCronExpression().trim())) {
                    throw new InvalidRequestException("Invalid cron expression syntax");
                }
            }
        }
    }

    private JobResponse mapToResponse(Job job) {
        return new JobResponse(
                job.getPublicId(),
                job.getProject().getPublicId(),
                job.getName(),
                job.getDescription(),
                job.isEnabled(),
                job.getHttpMethod(),
                job.getTargetUrl(),
                job.getRequestHeaders(),
                job.getRequestBody(),
                job.getTimeoutSeconds(),
                job.getRetryMaxAttempts(),
                job.getRetryStrategy(),
                job.getRetryBaseDelaySeconds(),
                job.getScheduleType(),
                job.getCronExpression(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCreatedBy(),
                job.getUpdatedBy(),
                job.getVersion()
        );
    }
}
