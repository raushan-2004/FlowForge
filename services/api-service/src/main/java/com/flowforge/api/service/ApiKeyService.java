package com.flowforge.api.service;

import com.flowforge.api.dto.ApiKeyCreateResponse;
import com.flowforge.api.dto.ApiKeyRequest;
import com.flowforge.api.dto.ApiKeyResponse;
import com.flowforge.api.exception.MembershipDeniedException;
import com.flowforge.api.exception.ResourceNotFoundException;
import com.flowforge.api.model.ApiKey;
import com.flowforge.api.model.ApiKeyStatus;
import com.flowforge.api.model.Project;
import com.flowforge.api.repository.ApiKeyRepository;
import com.flowforge.api.repository.ProjectRepository;
import com.flowforge.api.security.ApiKeyHasher;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;
    private final TenantAuthorizationService authorizationService;
    private final ApiKeyHasher apiKeyHasher;
    private final PublicIdGenerator publicIdGenerator;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${flowforge.security.apikey.environment:test}")
    private String environment;

    public ApiKeyService(
            ApiKeyRepository apiKeyRepository,
            ProjectRepository projectRepository,
            TenantAuthorizationService authorizationService,
            ApiKeyHasher apiKeyHasher,
            PublicIdGenerator publicIdGenerator,
            Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.apiKeyHasher = apiKeyHasher;
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

    private String generateSecureString(int bytesCount) {
        byte[] bytes = new byte[bytesCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public ApiKeyCreateResponse createKey(UUID tenantId, UUID projectId, ApiKeyRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canCreateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can create API keys");
        }

        Project project = projectRepository.findByPublicIdAndTenantPublicId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        String keyId = generateSecureString(16);
        String secret = generateSecureString(32);
        String secretHash = apiKeyHasher.hashSecret(secret);
        String displayPrefix = secret.substring(0, 6) + "...";

        Instant now = clock.instant();
        Instant expiryAt = null;
        if (request.getExpirySeconds() != null && request.getExpirySeconds() > 0) {
            expiryAt = now.plusSeconds(request.getExpirySeconds());
        }

        ApiKey apiKey = new ApiKey(
                publicIdGenerator.generate(),
                keyId,
                displayPrefix,
                secretHash,
                project,
                project.getTenant(),
                ApiKeyStatus.ACTIVE,
                expiryAt,
                context.getUserPublicId(),
                now
        );

        apiKey = apiKeyRepository.save(apiKey);

        String fullKey = "ff_" + environment + "." + keyId + "." + secret;
        return new ApiKeyCreateResponse(mapToResponse(apiKey), fullKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getKeys(UUID tenantId, UUID projectId) {
        getActiveTenantContext();

        projectRepository.findByPublicIdAndTenantPublicId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return apiKeyRepository.findAllByProjectPublicIdAndTenantPublicId(projectId, tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getKey(UUID tenantId, UUID projectId, UUID keyPublicId) {
        getActiveTenantContext();

        ApiKey apiKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (!apiKey.getProject().getPublicId().equals(projectId)) {
            throw new ResourceNotFoundException("API key not found");
        }

        return mapToResponse(apiKey);
    }

    @Transactional
    public ApiKeyResponse revokeKey(UUID tenantId, UUID projectId, UUID keyPublicId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canUpdateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can revoke API keys");
        }

        ApiKey apiKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (!apiKey.getProject().getPublicId().equals(projectId)) {
            throw new ResourceNotFoundException("API key not found");
        }

        apiKey.revoke(context.getUserPublicId());
        apiKey = apiKeyRepository.save(apiKey);

        return mapToResponse(apiKey);
    }

    @Transactional
    public ApiKeyCreateResponse rotateKey(UUID tenantId, UUID projectId, UUID keyPublicId, ApiKeyRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canUpdateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can rotate API keys");
        }

        ApiKey oldKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (!oldKey.getProject().getPublicId().equals(projectId)) {
            throw new ResourceNotFoundException("API key not found");
        }

        // Revoke the old key first
        oldKey.revoke(context.getUserPublicId());
        apiKeyRepository.save(oldKey);

        // Generate the rotated key
        String keyId = generateSecureString(16);
        String secret = generateSecureString(32);
        String secretHash = apiKeyHasher.hashSecret(secret);
        String displayPrefix = secret.substring(0, 6) + "...";

        Instant now = clock.instant();
        Instant expiryAt = null;
        if (request.getExpirySeconds() != null && request.getExpirySeconds() > 0) {
            expiryAt = now.plusSeconds(request.getExpirySeconds());
        }

        ApiKey newKey = new ApiKey(
                publicIdGenerator.generate(),
                keyId,
                displayPrefix,
                secretHash,
                oldKey.getProject(),
                oldKey.getTenant(),
                ApiKeyStatus.ACTIVE,
                expiryAt,
                context.getUserPublicId(),
                now
        );

        newKey = apiKeyRepository.save(newKey);

        String fullKey = "ff_" + environment + "." + keyId + "." + secret;
        return new ApiKeyCreateResponse(mapToResponse(newKey), fullKey);
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getPublicId(),
                apiKey.getKeyId(),
                apiKey.getDisplayPrefix(),
                apiKey.getStatus(),
                apiKey.getCreatedBy(),
                apiKey.getUpdatedBy(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt(),
                apiKey.getExpiryAt(),
                apiKey.getLastUsedAt()
        );
    }
}
