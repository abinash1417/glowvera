package com.glowvera.security;

import com.glowvera.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter writer;

    public RestAuthenticationEntryPoint(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        boolean expired = request.getAttribute(JwtAuthenticationFilter.SESSION_EXPIRED_ATTR) != null;
        writer.write(response, 401, ErrorCode.UNAUTHORIZED.name(),
                expired ? "Session expired. Please log in again." : "Authentication required");
    }
}
