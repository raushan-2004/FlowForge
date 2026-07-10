package com.flowforge.api.controller;

import com.flowforge.api.dto.LoginRequest;
import com.flowforge.api.dto.LoginResponse;
import com.flowforge.api.dto.RegisterRequest;
import com.flowforge.api.dto.UserResponse;
import com.flowforge.api.model.User;
import com.flowforge.api.service.HumanAuthenticationService;
import com.flowforge.api.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final HumanAuthenticationService authenticationService;

    public AuthController(
            RegistrationService registrationService,
            HumanAuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
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
}
