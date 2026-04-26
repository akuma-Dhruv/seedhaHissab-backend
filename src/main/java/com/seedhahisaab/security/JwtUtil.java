package com.seedhahisaab.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.expiration.ms:86400000}") long expirationMs,
            Environment environment) {
        String secret = System.getenv("SESSION_SECRET");
        
        // If not set as environment variable, try application properties
        if (secret == null || secret.isBlank()) {
            secret = environment.getProperty("jwt.secret");
        }
        
        // Development fallback - generate a default secret
        if (secret == null || secret.isBlank()) {
            String profile = environment.getProperty("spring.profiles.active", "default");
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                throw new IllegalStateException(
                        "SESSION_SECRET environment variable is required in production. " +
                        "Please set SESSION_SECRET or jwt.secret property before starting the application.");
            }
            // Use default for development
            secret = "dev-secret-key-minimum-32-characters-long-for-jwt";
            System.out.println("⚠️  WARNING: Using default JWT secret for development. " +
                    "Set SESSION_SECRET environment variable or jwt.secret property for production.");
        }
        
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "SESSION_SECRET/jwt.secret must be at least 32 characters long for security. " +
                    "Current length: " + secret.length() + " characters.");
        }
        
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
