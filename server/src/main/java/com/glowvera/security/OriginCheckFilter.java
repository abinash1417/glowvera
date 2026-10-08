package com.glowvera.security;

import com.glowvera.common.ErrorCode;
import com.glowvera.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final String allowedOrigin;
    private final ErrorResponseWriter writer;

    public OriginCheckFilter(AppProperties props, ErrorResponseWriter writer) {
        this.allowedOrigin = props.clientUrl().replaceAll("/+$", "");
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!SAFE_METHODS.contains(request.getMethod()) && origin != null && !origin.equals(allowedOrigin)) {
            writer.write(response, 403, ErrorCode.FORBIDDEN.name(), "Request origin not allowed");
            return;
        }
        chain.doFilter(request, response);
    }
}
