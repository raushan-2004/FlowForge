package com.flowforge.api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) 
            throws IOException, ServletException {
        // Return a generic structured 403 error response
        SecurityErrorWriter.writeErrorResponse(
                response, 
                HttpServletResponse.SC_FORBIDDEN, 
                "FORBIDDEN", 
                "Access to this resource is denied"
        );
    }
}
