package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.LoginRequest;
import com.flowforge.api.dto.RegisterRequest;
import com.flowforge.api.model.User;
import com.flowforge.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class IdentityAuthenticationIT extends BasePersistenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return new MutableClock(Instant.parse("2026-07-10T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @BeforeEach
    void setUp() {
        if (clock instanceof MutableClock) {
            ((MutableClock) clock).setInstant(Instant.parse("2026-07-10T12:00:00Z"));
        }
        userRepository.deleteAllInBatch();
    }

    @Test
    void testRegistrationFlow() throws Exception {
        RegisterRequest request = new RegisterRequest("register@flowforge.com", "supersecret123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.email", is("register@flowforge.com")));

        User savedUser = userRepository.findAll().get(0);
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$"); 
        assertThat(savedUser.getPasswordHash()).doesNotContain("supersecret123");
    }

    @Test
    void testRegistrationDuplicateEmailNormalizedRejection() throws Exception {
        RegisterRequest request1 = new RegisterRequest("  Register-Dup@flowforge.com  ", "supersecret123");
        RegisterRequest request2 = new RegisterRequest("register-dup@flowforge.com", "supersecret123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")));
    }

    @Test
    void testRegistrationDuplicateCaseVariantRejection() throws Exception {
        RegisterRequest request1 = new RegisterRequest("register-case@flowforge.com", "supersecret123");
        RegisterRequest request2 = new RegisterRequest("REGISTER-CASE@FLOWFORGE.COM", "differentpwd123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")));
    }

    @Test
    void testLoginFlowSuccessAndCredentialsErrors() throws Exception {
        RegisterRequest reg = new RegisterRequest("login@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("login@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        LoginRequest badPwd = new LoginRequest("login@flowforge.com", "wrongpwd123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPwd)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));

        LoginRequest unknownEmail = new LoginRequest("unknown@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password"))); 
    }

    @Test
    void testSuspendedUserLoginDenied() throws Exception {
        RegisterRequest reg = new RegisterRequest("suspended@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("suspended@flowforge.com").orElseThrow();
        user.suspend();
        userRepository.saveAndFlush(user);

        LoginRequest login = new LoginRequest("suspended@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SUSPENDED_ACCOUNT")));
    }

    @Test
    void testJwtTokensValidation() throws Exception {
        RegisterRequest reg = new RegisterRequest("jwt@flowforge.com", "supersecret123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("jwt@flowforge.com", "supersecret123");
        String loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        
        String token = objectMapper.readTree(loginRes).get("token").asText();

        mockMvc.perform(get("/api/v1/test-authenticated/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("authenticated")));

        if (clock instanceof MutableClock) {
            ((MutableClock) clock).advanceBySeconds(3601); 
        }
        mockMvc.perform(get("/api/v1/test-authenticated/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));

        if (clock instanceof MutableClock) {
            ((MutableClock) clock).setInstant(Instant.parse("2026-07-10T12:00:00Z"));
        }

        mockMvc.perform(get("/api/v1/test-authenticated/me")
                        .header("Authorization", "Bearer badtoken123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));

        mockMvc.perform(get("/api/v1/test-authenticated/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
