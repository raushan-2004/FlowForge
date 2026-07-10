package com.flowforge.api.service;

import com.flowforge.api.model.TenantRole;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantAuthorizationService {

    public boolean canManageProjects() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canCreateJobs() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER || role == TenantRole.ADMIN || role == TenantRole.DEVELOPER;
    }

    public boolean canViewExecutions() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            return false;
        }
        TenantRole role = context.getRole();
        return role == TenantRole.OWNER 
                || role == TenantRole.ADMIN 
                || role == TenantRole.DEVELOPER 
                || role == TenantRole.VIEWER;
    }
}
