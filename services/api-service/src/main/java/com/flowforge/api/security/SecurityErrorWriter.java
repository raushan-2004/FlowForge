package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SecurityErrorWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SecurityErrorWriter() {}

    public static void writeErrorResponse(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(code, message);
        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
