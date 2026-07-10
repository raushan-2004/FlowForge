package com.flowforge.api.service;

import com.flowforge.api.exception.DuplicateEmailException;
import com.flowforge.api.exception.InvalidRequestException;
import com.flowforge.api.model.User;
import com.flowforge.api.model.UserStatus;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicIdGenerator publicIdGenerator;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PublicIdGenerator publicIdGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.publicIdGenerator = publicIdGenerator;
    }

    @Transactional
    public User registerUser(String email, String plaintextPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidRequestException("Email is required");
        }
        if (plaintextPassword == null || plaintextPassword.length() < 8) {
            throw new InvalidRequestException("Password must be at least 8 characters long");
        }

        // Normalize email: trim and convert to lowercase using Locale.ROOT at application boundary
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // Validate email format
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidRequestException("Invalid email format");
        }

        // Defensive check, but database LOWER(email) unique index remains final authority
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(plaintextPassword);
        UUID publicId = publicIdGenerator.generate();

        User user = new User(publicId, normalizedEmail, passwordHash, UserStatus.ACTIVE);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate email registration handler
            throw new DuplicateEmailException("Email is already registered", e);
        }
    }
}
