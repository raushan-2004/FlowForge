package com.flowforge.api.security;

import com.flowforge.api.model.ApiKey;
import com.flowforge.api.model.ApiKeyStatus;
import com.flowforge.api.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.UUID;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final Clock clock;

    public ApiKeyAuthenticationFilter(
            ApiKeyRepository apiKeyRepository,
            ApiKeyHasher apiKeyHasher,
            Clock clock) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {

        String rawKey = request.getHeader("X-FlowForge-Api-Key");
        if (rawKey == null || rawKey.trim().isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String tokenPart = authHeader.substring(7).trim();
                if (tokenPart.startsWith("ff_")) {
                    rawKey = tokenPart;
                }
            }
        }

        if (rawKey == null || rawKey.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        rawKey = rawKey.trim();

        // Parse key: ff_<env>.<keyId>.<secret> or legacy ff_<env>*<keyId>*<secret>
        if (!rawKey.startsWith("ff_")) {
            SecurityErrorWriter.writeErrorResponse(
                    response, 
                    HttpServletResponse.SC_UNAUTHORIZED, 
                    "INVALID_API_KEY", 
                    "Invalid API key format"
            );
            return;
        }

        String[] parts;
        if (rawKey.contains(".")) {
            parts = rawKey.split("\\.");
        } else {
            parts = rawKey.split("\\*");
        }

        if (parts.length != 3) {
            SecurityErrorWriter.writeErrorResponse(
                    response, 
                    HttpServletResponse.SC_UNAUTHORIZED, 
                    "INVALID_API_KEY", 
                    "Invalid API key format"
            );
            return;
        }

        String keyId = parts[1];
        String secret = parts[2];

        try {
            ApiKey apiKey = apiKeyRepository.findByKeyIdWithProjectAndTenant(keyId).orElse(null);
            if (apiKey == null) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_UNAUTHORIZED, 
                        "INVALID_API_KEY", 
                        "Invalid API key"
                );
                return;
            }

            // Validate status
            if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_UNAUTHORIZED, 
                        "INVALID_API_KEY", 
                        "API key is not active"
                );
                return;
            }

            // Validate expiry
            if (apiKey.getExpiryAt() != null && apiKey.getExpiryAt().isBefore(clock.instant())) {
                apiKey.expire(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                apiKeyRepository.save(apiKey);

                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_UNAUTHORIZED, 
                        "INVALID_API_KEY", 
                        "API key has expired"
                );
                return;
            }

            // Verify secret hash
            if (!apiKeyHasher.matches(secret, apiKey.getSecretHash())) {
                SecurityErrorWriter.writeErrorResponse(
                        response, 
                        HttpServletResponse.SC_UNAUTHORIZED, 
                        "INVALID_API_KEY", 
                        "Invalid API key credentials"
                );
                return;
            }

            // Authenticated successfully -> Record usage
            apiKey.recordUsage(clock.instant());
            apiKeyRepository.save(apiKey);

            // Establish Principal
            AuthenticatedProjectPrincipal principal = new AuthenticatedProjectPrincipal(
                    apiKey.getProject().getPublicId(),
                    apiKey.getTenant().getPublicId()
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, 
                    null, 
                    Collections.emptyList()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityErrorWriter.writeErrorResponse(
                    response, 
                    HttpServletResponse.SC_UNAUTHORIZED, 
                    "INVALID_API_KEY", 
                    "An error occurred verifying the API key"
            );
        }
    }
}
