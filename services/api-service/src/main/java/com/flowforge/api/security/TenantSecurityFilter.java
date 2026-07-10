package com.flowforge.api.security;

import com.flowforge.api.model.Tenant;
import com.flowforge.api.model.TenantMembership;
import com.flowforge.api.model.TenantStatus;
import com.flowforge.api.repository.TenantMembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantSecurityFilter extends OncePerRequestFilter {

    private final TenantMembershipRepository membershipRepository;

    public TenantSecurityFilter(TenantMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. Route classification
        if (!isTenantScopedRoute(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Establish authentication presence
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal)) {
            // Let the request proceed; Spring Security will throw 401 later since route is secure
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) auth.getPrincipal();
        UUID userPublicId = principal.getUserPublicId();

        // 3. Parse X-FlowForge-Tenant header
        String tenantHeader = request.getHeader("X-FlowForge-Tenant");
        if (tenantHeader == null || tenantHeader.trim().isEmpty()) {
            SecurityErrorWriter.writeErrorResponse(
                    response, 
                    HttpServletResponse.SC_BAD_REQUEST, 
                    "MISSING_TENANT_HEADER", 
                    "Active tenant context selection header (X-FlowForge-Tenant) is required for this route"
            );
            return;
        }

        UUID tenantPublicId;
        try {
            tenantPublicId = UUID.fromString(tenantHeader.trim());
        } catch (IllegalArgumentException e) {
            SecurityErrorWriter.writeErrorResponse(
                    response, 
                    HttpServletResponse.SC_BAD_REQUEST, 
                    "MALFORMED_TENANT_IDENTIFIER", 
                    "Active tenant context selection header must be a valid UUID"
            );
            return;
        }

        // 4. Validate membership & tenant status
        try {
            TenantMembership membership = membershipRepository
                    .findByTenantPublicIdAndUserPublicId(tenantPublicId, userPublicId)
                    .orElse(null);

            if (membership == null) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_FORBIDDEN, 
                        "MEMBERSHIP_DENIED", 
                        "User does not hold membership in the selected tenant"
                );
                return;
            }

            Tenant tenant = membership.getTenant();
            if (tenant.getStatus() == TenantStatus.CLOSED) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_FORBIDDEN, 
                        "TENANT_NOT_FOUND", 
                        "The requested tenant has been closed"
                );
                return;
            }

            if (tenant.getStatus() == TenantStatus.SUSPENDED) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_FORBIDDEN, 
                        "TENANT_NOT_FOUND", 
                        "The requested tenant is suspended"
                );
                return;
            }

            // 5. Establish context
            TenantSecurityContext context = new TenantSecurityContext(
                    userPublicId, 
                    tenantPublicId, 
                    tenant.getId(), 
                    membership.getRole()
            );
            TenantSecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } finally {
            // 6. Guaranteed context cleanup
            TenantSecurityContextHolder.clear();
        }
    }

    private boolean isTenantScopedRoute(String uri) {
        return uri.startsWith("/api/v1/projects") 
                || uri.startsWith("/api/v1/jobs") 
                || uri.startsWith("/api/v1/executions") 
                || uri.startsWith("/api/v1/workflows")
                || uri.startsWith("/api/v1/test-tenant-scoped");
    }
}
