package com.flowforge.api.controller;

import com.flowforge.api.dto.LoginRequest;
import com.flowforge.api.dto.LoginResponse;
import com.flowforge.api.dto.RegisterRequest;
import com.flowforge.api.dto.UserResponse;
import com.flowforge.api.model.User;
import com.flowforge.api.model.TenantMembership;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.repository.TenantMembershipRepository;
import com.flowforge.api.security.AuthenticatedUserPrincipal;
import com.flowforge.api.service.HumanAuthenticationService;
import com.flowforge.api.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@SuppressWarnings("all")
public class AuthController {

    private final RegistrationService registrationService;
    private final HumanAuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;

    public AuthController(
            RegistrationService registrationService,
            HumanAuthenticationService authenticationService,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.registerUser(request.getEmail(), request.getPassword());
        UserResponse response = new UserResponse(user.getPublicId(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authenticationService.authenticate(request.getEmail(), request.getPassword());
        LoginResponse response = new LoginResponse(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        UUID userPublicId = ((AuthenticatedUserPrincipal) auth.getPrincipal()).getUserPublicId();
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        List<TenantMembership> memberships = membershipRepository.findAllByUserPublicId(userPublicId);
        List<Map<String, Object>> tenantsList = memberships.stream().map(m -> {
            Map<String, Object> t = new HashMap<>();
            t.put("id", m.getTenant().getPublicId().toString());
            t.put("name", m.getTenant().getName());
            t.put("role", m.getRole().toString());
            return t;
        }).collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("id", user.getPublicId().toString());
        body.put("email", user.getEmail());
        body.put("tenants", tenantsList);

        return ResponseEntity.ok(body);
    }
}
