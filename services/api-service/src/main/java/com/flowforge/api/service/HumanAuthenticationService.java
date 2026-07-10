package com.flowforge.api.service;

import com.flowforge.api.exception.InvalidCredentialsException;
import com.flowforge.api.exception.SuspendedAccountException;
import com.flowforge.api.model.User;
import com.flowforge.api.model.UserStatus;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class HumanAuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public HumanAuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public String authenticate(String email, String plaintextPassword) {
        if (email == null || plaintextPassword == null) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Normalize email: trim and convert to lowercase using Locale.ROOT at application boundary
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // Retrieve user
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password first to prevent account enumeration
        if (!passwordEncoder.matches(plaintextPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check account status only after successful credentials validation
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new SuspendedAccountException("Account is suspended");
        }

        // Generate token
        return jwtService.generateToken(user.getPublicId());
    }
}
