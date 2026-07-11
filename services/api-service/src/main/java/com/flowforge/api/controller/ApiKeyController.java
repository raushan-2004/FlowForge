package com.flowforge.api.controller;

import com.flowforge.api.dto.ApiKeyCreateResponse;
import com.flowforge.api.dto.ApiKeyRequest;
import com.flowforge.api.dto.ApiKeyResponse;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import com.flowforge.api.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    private UUID getActiveTenantId() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            throw new com.flowforge.api.exception.MembershipDeniedException("Tenant context required");
        }
        return context.getTenantPublicId();
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreateResponse> createKey(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody ApiKeyRequest request) {
        UUID tenantId = getActiveTenantId();
        ApiKeyCreateResponse response = apiKeyService.createKey(tenantId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getKeys(
            @PathVariable("projectId") UUID projectId) {
        UUID tenantId = getActiveTenantId();
        List<ApiKeyResponse> response = apiKeyService.getKeys(tenantId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{keyId}")
    public ResponseEntity<ApiKeyResponse> getKey(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("keyId") UUID keyId) {
        UUID tenantId = getActiveTenantId();
        ApiKeyResponse response = apiKeyService.getKey(tenantId, projectId, keyId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<ApiKeyResponse> revokeKey(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("keyId") UUID keyId) {
        UUID tenantId = getActiveTenantId();
        ApiKeyResponse response = apiKeyService.revokeKey(tenantId, projectId, keyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<ApiKeyCreateResponse> rotateKey(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("keyId") UUID keyId,
            @Valid @RequestBody ApiKeyRequest request) {
        UUID tenantId = getActiveTenantId();
        ApiKeyCreateResponse response = apiKeyService.rotateKey(tenantId, projectId, keyId, request);
        return ResponseEntity.ok(response);
    }
}
