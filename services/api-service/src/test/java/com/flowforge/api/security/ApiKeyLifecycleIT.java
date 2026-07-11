package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.ApiKeyCreateResponse;
import com.flowforge.api.dto.ApiKeyRequest;
import com.flowforge.api.dto.ApiKeyResponse;
import com.flowforge.api.dto.MembershipRequest;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ApiKeyLifecycleIT extends BasePersistenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private User ownerUser;
    private String ownerToken;

    private Tenant tenantA;
    private Tenant tenantB;

    private Project projectA;
    private Project projectB;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return new MutableClock(Instant.parse("2026-07-11T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @BeforeEach
    void setUp() {
        if (clock instanceof MutableClock) {
            ((MutableClock) clock).setInstant(Instant.parse("2026-07-11T12:00:00Z"));
        }

        apiKeyRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Create Owner
        ownerUser = userRepository.save(new User(publicIdGenerator.generate(), "owner@flowforge.com", "hash", UserStatus.ACTIVE));
        ownerToken = jwtService.generateToken(ownerUser.getPublicId());

        // Create Tenant & Project A
        tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        membershipRepository.save(new TenantMembership(tenantA, ownerUser, TenantRole.OWNER));
        projectA = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A", ProjectStatus.ACTIVE, ownerUser.getPublicId()));

        // Create Tenant & Project B
        tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        membershipRepository.save(new TenantMembership(tenantB, ownerUser, TenantRole.OWNER));
        projectB = projectRepository.save(new Project(publicIdGenerator.generate(), tenantB, "Project B", ProjectStatus.ACTIVE, ownerUser.getPublicId()));
    }

    @Test
    void testApiKeyCreateRotateRevoke() throws Exception {
        ApiKeyRequest createReq = new ApiKeyRequest();

        // 1. Create API key -> returns full plaintext token once
        String createRes = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.metadata.publicId").exists())
                .andExpect(jsonPath("$.metadata.displayPrefix").exists())
                .andExpect(jsonPath("$.metadata.status", is("ACTIVE")))
                .andReturn().getResponse().getContentAsString();

        ApiKeyCreateResponse response = objectMapper.readValue(createRes, ApiKeyCreateResponse.class);
        String plaintextKey = response.getToken();
        UUID keyPublicId = response.getMetadata().getPublicId();

        // 2. Fetch API key details -> does NOT expose secret
        mockMvc.perform(get("/api/v1/projects/" + projectA.getPublicId() + "/keys/" + keyPublicId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId", is(keyPublicId.toString())))
                .andExpect(jsonPath("$.secretHash").doesNotExist()) // Never exposed
                .andExpect(jsonPath("$.secret").doesNotExist());

        // Verify database persistence -> plaintext secret is NOT stored
        ApiKey persistedKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantA.getPublicId()).orElseThrow();
        assertThat(persistedKey.getSecretHash()).startsWith("{sha256}"); // SHA-256 hashed
        assertThat(persistedKey.getSecretHash()).isNotEqualTo(plaintextKey);
        assertThat(persistedKey.getCreatedBy()).isEqualTo(ownerUser.getPublicId());

        // 3. Rotate key -> revokes old key, returns new full key
        String rotateRes = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys/" + keyPublicId + "/rotate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(plaintextKey)))
                .andExpect(jsonPath("$.metadata.status", is("ACTIVE")))
                .andReturn().getResponse().getContentAsString();

        // Check old key is revoked
        ApiKey rotatedOldKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantA.getPublicId()).orElseThrow();
        assertThat(rotatedOldKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(rotatedOldKey.getUpdatedBy()).isEqualTo(ownerUser.getPublicId());

        ApiKeyCreateResponse rotateResponse = objectMapper.readValue(rotateRes, ApiKeyCreateResponse.class);
        UUID newKeyPublicId = rotateResponse.getMetadata().getPublicId();

        // 4. Revoke key -> transitions status to REVOKED
        mockMvc.perform(delete("/api/v1/projects/" + projectA.getPublicId() + "/keys/" + newKeyPublicId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")));

        ApiKey revokedKey = apiKeyRepository.findByPublicIdAndTenantPublicId(newKeyPublicId, tenantA.getPublicId()).orElseThrow();
        assertThat(revokedKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    void testApiKeyAuthenticationAndValidation() throws Exception {
        ApiKeyRequest createReq = new ApiKeyRequest(3600L); // 1 hour expiry
        String createRes = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        ApiKeyCreateResponse response = objectMapper.readValue(createRes, ApiKeyCreateResponse.class);
        String plaintextKey = response.getToken();
        UUID keyPublicId = response.getMetadata().getPublicId();

        // 1. Successful Authentication (via X-FlowForge-Api-Key header)
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", plaintextKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Project A")));

        // Verify last_used_at update
        ApiKey verifiedKey = apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantA.getPublicId()).orElseThrow();
        assertThat(verifiedKey.getLastUsedAt()).isNotNull();

        // 2. Malformed key format -> gets 401
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", "ff_test.invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_API_KEY")));

        // 3. Wrong secret component -> gets 401
        String[] parts = plaintextKey.split("\\.");
        String wrongSecretKey = parts[0] + "." + parts[1] + ".wrongsecret";
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", wrongSecretKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_API_KEY")));

        // 4. Revoked key authentication -> gets 401
        apiKeyRepository.findByPublicIdAndTenantPublicId(keyPublicId, tenantA.getPublicId()).ifPresent(k -> {
            k.revoke(ownerUser.getPublicId());
            apiKeyRepository.saveAndFlush(k);
        });
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", plaintextKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_API_KEY")));

        // 5. Expired key (advance controllable clock) -> gets 401
        String freshRes = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiKeyRequest(10L)))) // 10s expiry
                .andReturn().getResponse().getContentAsString();
        
        ApiKeyCreateResponse freshKeyResponse = objectMapper.readValue(freshRes, ApiKeyCreateResponse.class);
        String freshPlaintextKey = freshKeyResponse.getToken();

        // Advance clock by 11 seconds
        if (clock instanceof MutableClock) {
            ((MutableClock) clock).advanceBySeconds(11);
        }

        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", freshPlaintextKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_API_KEY")));
    }

    @Test
    void testProjectAndTenantIsolation() throws Exception {
        // Create Key in Tenant A for Project A
        String createResA = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiKeyRequest())))
                .andReturn().getResponse().getContentAsString();
        String apiKeyA = objectMapper.readValue(createResA, ApiKeyCreateResponse.class).getToken();

        // 1. Tenant listing isolation: querying GET /projects with Key A returns ONLY Project A
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", apiKeyA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Project A")));

        // 2. Direct resource isolation: fetching Project B (Tenant B) using Key A -> returns 404
        mockMvc.perform(get("/api/v1/projects/" + projectB.getPublicId())
                        .header("X-FlowForge-Api-Key", apiKeyA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void testApiKeyAuthorizationLimits() throws Exception {
        // Create Key in Tenant A for Project A
        String createResA = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiKeyRequest())))
                .andReturn().getResponse().getContentAsString();
        String apiKeyA = objectMapper.readValue(createResA, ApiKeyCreateResponse.class).getToken();

        // Attempting a human OWNER level action (add membership) using Project Key A -> gets 403 Forbidden
        MembershipRequest req = new MembershipRequest(ownerUser.getPublicId(), TenantRole.DEVELOPER);
        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getPublicId() + "/members")
                        .header("X-FlowForge-Api-Key", apiKeyA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED"))); // Capability restricted
    }

    @Test
    void testLegacyBcryptKeyAuthentication() throws Exception {
        // Manually encode using bcrypt
        String legacyKeyId = "legacyKeyId";
        String legacySecret = "legacySecret123";
        String bcryptHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(legacySecret);
        
        ApiKey legacyApiKey = new ApiKey(
                publicIdGenerator.generate(),
                legacyKeyId,
                "legPrefix",
                bcryptHash,
                projectA,
                tenantA,
                ApiKeyStatus.ACTIVE,
                null,
                ownerUser.getPublicId()
        );
        apiKeyRepository.saveAndFlush(legacyApiKey);
        
        // Assemble legacy full key using '*' delimiter
        String legacyFullKey = "ff_test*" + legacyKeyId + "*" + legacySecret;
        
        // Authenticate -> must succeed (backward compatibility check)
        mockMvc.perform(get("/api/v1/projects")
                        .header("X-FlowForge-Api-Key", legacyFullKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Project A")));
    }
}
