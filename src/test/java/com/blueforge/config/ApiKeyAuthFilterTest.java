package com.blueforge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthFilter filterWithKey(String configuredApiKey) {
        return new ApiKeyAuthFilter(configuredApiKey, new String[] {"http://localhost:5173"}, new ObjectMapper());
    }

    @Test
    void isDisabledEntirelyWhenNoApiKeyIsConfigured() {
        ApiKeyAuthFilter filter = filterWithKey("");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doesNotFilterNonApiPaths() {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doesNotFilterPreflightOptionsRequests() {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(request.getMethod()).thenReturn("OPTIONS");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void appliesToApiPathsWhenConfigured() {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(request.getMethod()).thenReturn("GET");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void allowsRequestThroughWhenApiKeyMatches() throws Exception {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getHeader("X-API-Key")).thenReturn("secret");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(401);
    }

    @Test
    void rejectsRequestWithMissingOrWrongApiKey() throws Exception {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getHeader("X-API-Key")).thenReturn("wrong-key");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(401);
        assertThat(body.toString()).contains("Missing or invalid API key.");
    }

    @Test
    void addsCorsHeaderOnRejectionWhenOriginIsAllowed() throws Exception {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Origin")).thenReturn("http://localhost:5173");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    @Test
    void omitsCorsHeaderOnRejectionWhenOriginIsNotAllowed() throws Exception {
        ApiKeyAuthFilter filter = filterWithKey("secret");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Origin")).thenReturn("http://evil.example.com");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setHeader("Access-Control-Allow-Origin", "http://evil.example.com");
    }
}
