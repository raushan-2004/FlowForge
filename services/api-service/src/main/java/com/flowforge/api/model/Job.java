package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    private JobHttpMethod httpMethod;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Convert(converter = HeadersConverter.class)
    @Column(name = "request_headers")
    private Map<String, String> requestHeaders = new HashMap<>();

    @Column(name = "request_body")
    private String requestBody;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    @Column(name = "retry_max_attempts")
    private Integer retryMaxAttempts;

    @Column(name = "retry_strategy")
    private String retryStrategy;

    @Column(name = "retry_base_delay_seconds")
    private Integer retryBaseDelaySeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private JobScheduleType scheduleType;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    protected Job() {}

    public Job(UUID publicId, Project project, String name, String description, boolean enabled,
               JobHttpMethod httpMethod, String targetUrl, Map<String, String> requestHeaders, String requestBody,
               int timeoutSeconds, Integer retryMaxAttempts, String retryStrategy, Integer retryBaseDelaySeconds,
               JobScheduleType scheduleType, String cronExpression, JobStatus status, UUID createdBy) {
        this(publicId, project, name, description, enabled, httpMethod, targetUrl, requestHeaders, requestBody,
                timeoutSeconds, retryMaxAttempts, retryStrategy, retryBaseDelaySeconds, scheduleType, cronExpression,
                status, createdBy, Instant.now());
    }

    public Job(UUID publicId, Project project, String name, String description, boolean enabled,
               JobHttpMethod httpMethod, String targetUrl, Map<String, String> requestHeaders, String requestBody,
               int timeoutSeconds, Integer retryMaxAttempts, String retryStrategy, Integer retryBaseDelaySeconds,
               JobScheduleType scheduleType, String cronExpression, JobStatus status, UUID createdBy, Instant createdAt) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (project == null) throw new IllegalArgumentException("project cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (httpMethod == null) throw new IllegalArgumentException("httpMethod cannot be null");
        if (targetUrl == null) throw new IllegalArgumentException("targetUrl cannot be null");
        if (scheduleType == null) throw new IllegalArgumentException("scheduleType cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdBy == null) throw new IllegalArgumentException("createdBy cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");

        this.publicId = publicId;
        this.project = project;
        this.name = name.trim();
        this.description = description;
        this.enabled = enabled;
        this.httpMethod = httpMethod;
        this.targetUrl = targetUrl;
        Map<String, String> normalized = new java.util.HashMap<>();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().trim().toLowerCase(), entry.getValue() != null ? entry.getValue().trim() : "");
                }
            }
        }
        this.requestHeaders = normalized;
        this.requestBody = requestBody;
        this.timeoutSeconds = timeoutSeconds;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryStrategy = retryStrategy;
        this.retryBaseDelaySeconds = retryBaseDelaySeconds;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JobHttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Integer getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public String getRetryStrategy() {
        return retryStrategy;
    }

    public Integer getRetryBaseDelaySeconds() {
        return retryBaseDelaySeconds;
    }

    public JobScheduleType getScheduleType() {
        return scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void updateDetails(String name, String description, boolean enabled, JobHttpMethod httpMethod,
                              String targetUrl, Map<String, String> requestHeaders, String requestBody,
                              int timeoutSeconds, Integer retryMaxAttempts, String retryStrategy, Integer retryBaseDelaySeconds,
                              JobScheduleType scheduleType, String cronExpression, JobStatus status, UUID updatedBy, Instant updatedAt) {
        if (name != null) this.name = name.trim();
        this.description = description;
        this.enabled = enabled;
        if (httpMethod != null) this.httpMethod = httpMethod;
        if (targetUrl != null) this.targetUrl = targetUrl;
        Map<String, String> normalized = new java.util.HashMap<>();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().trim().toLowerCase(), entry.getValue() != null ? entry.getValue().trim() : "");
                }
            }
        }
        this.requestHeaders = normalized;
        this.requestBody = requestBody;
        this.timeoutSeconds = timeoutSeconds;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryStrategy = retryStrategy;
        this.retryBaseDelaySeconds = retryBaseDelaySeconds;
        if (scheduleType != null) this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        if (status != null) this.status = status;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public void archive(UUID updatedBy, Instant updatedAt) {
        this.status = JobStatus.ARCHIVED;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Job job = (Job) o;
        return publicId != null && publicId.equals(job.getPublicId());
    }

    @Override
    public int hashCode() {
        return publicId != null ? publicId.hashCode() : 0;
    }
}
