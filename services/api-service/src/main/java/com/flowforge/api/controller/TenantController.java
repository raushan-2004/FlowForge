package com.flowforge.api.controller;

import com.flowforge.api.dto.TenantRequest;
import com.flowforge.api.dto.TenantResponse;
import com.flowforge.api.security.AuthenticatedUserPrincipal;
import com.flowforge.api.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    private UUID getAuthenticatedUserPublicId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return ((AuthenticatedUserPrincipal) auth.getPrincipal()).getUserPublicId();
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody TenantRequest request, 
            Authentication auth) {
        UUID userPublicId = getAuthenticatedUserPublicId(auth);
        TenantResponse response = tenantService.createTenant(request, userPublicId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> listTenants(Authentication auth) {
        // Enforce authentication presence
        getAuthenticatedUserPublicId(auth);
        List<TenantResponse> response = tenantService.listTenants();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(
            @PathVariable("tenantId") UUID tenantId, 
            Authentication auth) {
        getAuthenticatedUserPublicId(auth);
        TenantResponse response = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable("tenantId") UUID tenantId,
            @RequestBody TenantRequest request,
            Authentication auth) {
        UUID userPublicId = getAuthenticatedUserPublicId(auth);
        TenantResponse response = tenantService.updateTenant(tenantId, request, userPublicId);
        return ResponseEntity.ok(response);
    }
}
