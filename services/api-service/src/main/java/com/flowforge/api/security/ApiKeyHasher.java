package com.flowforge.api.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class ApiKeyHasher {

    private final PasswordEncoder passwordEncoder;

    public ApiKeyHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hashes the plaintext secret using high-performance SHA-256 with standard base64 encoding.
     */
    public String hashSecret(String secret) {
        if (secret == null) throw new IllegalArgumentException("secret cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return "{sha256}" + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm is missing", e);
        }
    }

    /**
     * Checks if the incoming secret matches the persisted hash in constant-time.
     * Falls back to BCrypt if the stored hash was created by a legacy build.
     */
    public boolean matches(String secret, String persistedHash) {
        if (secret == null || persistedHash == null) {
            return false;
        }

        // SHA-256 Check
        if (persistedHash.startsWith("{sha256}")) {
            String incomingHash = hashSecret(secret);
            return MessageDigest.isEqual(
                    incomingHash.getBytes(StandardCharsets.UTF_8),
                    persistedHash.getBytes(StandardCharsets.UTF_8)
            );
        }

        // Legacy BCrypt fallback
        if (persistedHash.startsWith("$2a$") || persistedHash.startsWith("$2b$")) {
            return passwordEncoder.matches(secret, persistedHash);
        }

        return false;
    }
}
