package com.kanbana.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    // Injected from application.properties — never hardcoded
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // Build a consistent SecretKey from the configured secret string
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate a signed JWT containing the user's ID and username
    public String generateToken(UUID userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())         // subject = user ID
                .claim("username", username)         // extra claim — useful for logging
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    // Extract the user ID from a valid token
    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    // Extract the username from a valid token
    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    // Validate token — throws JwtException if invalid or expired
    public boolean isValid(String token) {
        try {
            extractClaims(token);   // throws if invalid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Parse and return all claims — throws JwtException on any failure
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
