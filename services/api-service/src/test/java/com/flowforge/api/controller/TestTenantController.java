package com.flowforge.api.controller;

import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import com.flowforge.api.service.TenantAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestTenantController {

    private final TenantAuthorizationService authorizationService;

    public TestTenantController(TenantAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/api/v1/test-tenant-scoped/projects")
    public ResponseEntity<Map<String, Object>> getProjects() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No tenant context");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userPublicId", context.getUserPublicId());
        body.put("tenantPublicId", context.getTenantPublicId());
        body.put("role", context.getRole());
        body.put("canManageProjects", authorizationService.canManageProjects());
        body.put("canCreateJobs", authorizationService.canCreateJobs());
        body.put("canViewExecutions", authorizationService.canViewExecutions());

        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/v1/test-authenticated/me")
    public ResponseEntity<Map<String, Object>> getMe() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "authenticated");
        return ResponseEntity.ok(body);
    }
}
