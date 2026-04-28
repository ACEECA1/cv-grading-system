package org.djezzy.pfe.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String CALLBACKS_PATH_PREFIX = "/api/callbacks/";
    private static final String API_KEY_HEADER = "X-API-KEY";

    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith(CALLBACKS_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String expectedApiKey = appProperties.getCallback().getApiKey();
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (expectedApiKey == null || expectedApiKey.isBlank() || !expectedApiKey.equals(providedApiKey)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

