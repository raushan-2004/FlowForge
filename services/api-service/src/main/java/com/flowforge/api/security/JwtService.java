package com.flowforge.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Clock clock;
    private final String secretString;
    private final long expirationMs;
    private SecretKey secretKey;

    public JwtService(
            Clock clock,
            @Value("${flowforge.security.jwt.secret}") String secretString,
            @Value("${flowforge.security.jwt.expiration-ms:3600000}") long expirationMs) {
        this.clock = clock;
        this.secretString = secretString;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    public void init() {
        if (secretString == null || secretString.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT signing secret (flowforge.security.jwt.secret) must not be blank.");
        }
        
        byte[] keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // 256 bits minimum key strength requirement
            throw new IllegalStateException(
                    "JWT signing secret is too weak. Minimum strength requirement is 256 bits (32 bytes). Provided length: " 
                    + keyBytes.length + " bytes."
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UUID userPublicId) {
        if (userPublicId == null) {
            throw new IllegalArgumentException("userPublicId cannot be null");
        }
        
        Instant now = clock.instant();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(userPublicId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public UUID extractUserPublicId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // Re-throw a generic parsing exception to protect signing internals
            throw new InvalidTokenException("Invalid or expired JWT token", e);
        }
    }

    // Generic domain-level runtime exception
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
