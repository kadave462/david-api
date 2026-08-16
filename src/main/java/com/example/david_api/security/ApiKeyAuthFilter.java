package com.example.david_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// Guards every /api/v1/ingest/** request (GET and POST alike — the GET
// endpoints used to have no check at all) with the shared pharmacy-client
// API key. This replaces the old per-method "apiKey.equals(configuredKey)"
// checks that used to live in IngestionService: those only ran on the POST
// methods, left the GET ones wide open, and used a non-constant-time
// String.equals comparison. Runs before the JWT filter's target path, so it
// only ever looks at /api/v1/ingest/** and leaves everything else alone.
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String INGEST_PATH_PREFIX = "/api/v1/ingest/";
    private static final String HEADER = "X-Api-Key";

    private final String configuredApiKey;

    public ApiKeyAuthFilter(@Value("${app.api-key}") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // /health is a plain liveness probe (used by uptime checks) — no
        // pharmacy data behind it, so it stays unauthenticated.
        if (!path.startsWith(INGEST_PATH_PREFIX) || path.equals(INGEST_PATH_PREFIX + "health")) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER);
        if (providedKey != null && constantTimeEquals(providedKey, configuredApiKey)) {
            var authToken = new UsernamePasswordAuthenticationToken(
                    "pharmacy-client", null, List.of(new SimpleGrantedAuthority("ROLE_INGEST")));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
