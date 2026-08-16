package com.example.david_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

// Issues and validates the JWTs handed out by AuthController on login and
// checked by JwtAuthFilter on every request to a protected endpoint.
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt-secret}") String configuredSecret,
            @Value("${app.jwt-expiration-ms}") long expirationMs) {
        this.expirationMs = expirationMs;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            log.warn("app.jwt-secret is not set — generating a random signing key for this run. "
                    + "Set the JWT_SECRET environment variable in production so tokens survive "
                    + "restarts and stay valid across multiple app instances.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(configuredSecret));
        }
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    // Returns the username (subject) if the token is valid and unexpired,
    // or empty if it's missing, malformed, expired, or signed with a
    // different key.
    public java.util.Optional<String> validateAndGetSubject(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return java.util.Optional.ofNullable(subject);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}
