package com.flowforge.api.service;

import com.flowforge.api.model.TenantRole;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantAuthorizationService {

    private TenantSecurityContext getContext() {
        return TenantSecurityContextHolder.getContext();
    }

    public boolean canManageMembers() {
        TenantSecurityContext context = getContext();
        if (context == null || context.isAutomation()) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN;
    }

    public boolean canCreateProjects() {
        TenantSecurityContext context = getContext();
        if (context == null || context.isAutomation()) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canUpdateProjects() {
        TenantSecurityContext context = getContext();
        if (context == null || context.isAutomation()) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canArchiveProjects() {
        TenantSecurityContext context = getContext();
        if (context == null || context.isAutomation()) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canManageProjects() {
        return canCreateProjects();
    }

    public boolean canCreateJobs() {
        TenantSecurityContext context = getContext();
        if (context == null || context.isAutomation()) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canViewExecutions() {
        TenantSecurityContext context = getContext();
        if (context == null) {
            return false;
        }
        if (context.isAutomation()) {
            return true; // Automation API Keys are allowed to view executions (within their project)
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER 
                || role == TenantRole.ADMIN 
                || role == TenantRole.DEVELOPER 
                || role == TenantRole.VIEWER;
    }

    // --- Extensible Automation-Only Permissions ---

    public boolean canSubmitJobs() {
        TenantSecurityContext context = getContext();
        return context != null && context.isAutomation();
    }

    public boolean canReadExecutions() {
        TenantSecurityContext context = getContext();
        return context != null && (context.isAutomation() || canViewExecutions());
    }

    public boolean canReadProjectMetadata() {
        TenantSecurityContext context = getContext();
        return context != null; // Both human users and project keys can read metadata
    }
}
