package com.blueforge.config;

import com.blueforge.controller.ApiExceptionHandler.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// A single shared secret, not per-user accounts - proportionate to "simple API-key auth" for a
// single-user portfolio demo. Disabled entirely (every request passes through) whenever
// blueforge.security.api-key is left unset, so local dev/docker-compose need no setup.
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final String configuredApiKey;
    private final List<String> allowedOrigins;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(
            @Value("${blueforge.security.api-key:}") String configuredApiKey,
            @Value("${blueforge.cors.allowed-origins}") String[] allowedOrigins,
            ObjectMapper objectMapper) {
        this.configuredApiKey = configuredApiKey;
        this.allowedOrigins = Arrays.asList(allowedOrigins);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return configuredApiKey.isBlank()
                || !request.getRequestURI().startsWith("/api/")
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (configuredApiKey.equals(request.getHeader(API_KEY_HEADER))) {
            filterChain.doFilter(request, response);
            return;
        }

        // This response is written before the request reaches DispatcherServlet, so it bypasses
        // Spring's WebMvcConfigurer-based CORS handling entirely - add the header ourselves or the
        // browser reports a CORS failure instead of surfacing the real 401 to the frontend.
        String origin = request.getHeader("Origin");
        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Missing or invalid API key.")));
    }
}
