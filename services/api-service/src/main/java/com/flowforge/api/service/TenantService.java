package com.flowforge.api.service;

import com.flowforge.api.dto.TenantRequest;
import com.flowforge.api.dto.TenantResponse;
import com.flowforge.api.exception.*;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.TenantMembershipRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PublicIdGenerator publicIdGenerator;

    public TenantService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            PublicIdGenerator publicIdGenerator) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.publicIdGenerator = publicIdGenerator;
    }

    @Transactional
    public TenantResponse createTenant(TenantRequest request, UUID creatorPublicId) {
        User creator = userRepository.findByPublicId(creatorPublicId)
                .orElseThrow(() -> new InvalidRequestException("Creator user not found"));

        if (creator.getStatus() == UserStatus.SUSPENDED) {
            throw new SuspendedAccountException("Suspended users cannot create tenants");
        }

        String name = request.getName().trim();
        if (tenantRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateTenantNameException("Tenant name is already taken");
        }

        Tenant tenant = new Tenant(publicIdGenerator.generate(), name, TenantStatus.ACTIVE, creatorPublicId);
        tenant = tenantRepository.save(tenant);

        TenantMembership membership = new TenantMembership(tenant, creator, TenantRole.OWNER);
        membershipRepository.save(membership);

        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findByPublicId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantResponse updateTenant(UUID tenantId, TenantRequest request, UUID userPublicId) {
        Tenant tenant = tenantRepository.findByPublicId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        TenantMembership membership = membershipRepository.findByTenantPublicIdAndUserPublicId(tenantId, userPublicId)
                .orElseThrow(() -> new MembershipDeniedException("Access denied"));

        if (membership.getRole() != TenantRole.OWNER && membership.getRole() != TenantRole.ADMIN) {
            throw new MembershipDeniedException("Only OWNER or ADMIN roles can update tenant properties");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(tenant.getName())) {
                if (tenantRepository.findByNameIgnoreCase(newName).isPresent()) {
                    throw new DuplicateTenantNameException("Tenant name is already taken");
                }
                tenant.changeName(newName, userPublicId);
            }
        }

        if (request.getStatus() != null) {
            try {
                TenantStatus newStatus = TenantStatus.valueOf(request.getStatus().trim().toUpperCase());
                if (newStatus != tenant.getStatus()) {
                    switch (newStatus) {
                        case ACTIVE -> tenant.activate(userPublicId);
                        case SUSPENDED -> tenant.suspend(userPublicId);
                        case CLOSED -> tenant.close(userPublicId);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Invalid tenant status: " + request.getStatus());
            }
        }

        tenant = tenantRepository.save(tenant);
        return mapToResponse(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getPublicId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedBy(),
                tenant.getUpdatedBy(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
