package com.flowforge.api.service;

import com.flowforge.api.dto.MembershipRequest;
import com.flowforge.api.dto.MembershipResponse;
import com.flowforge.api.exception.InvalidRequestException;
import com.flowforge.api.exception.MembershipDeniedException;
import com.flowforge.api.exception.TenantNotFoundException;
import com.flowforge.api.model.Tenant;
import com.flowforge.api.model.TenantMembership;
import com.flowforge.api.model.TenantRole;
import com.flowforge.api.model.User;
import com.flowforge.api.repository.TenantMembershipRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MembershipService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantAuthorizationService authorizationService;

    public MembershipService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            TenantAuthorizationService authorizationService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }

    private void validateTenantContext(UUID tenantId) {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null || !context.getTenantPublicId().equals(tenantId)) {
            throw new MembershipDeniedException("Invalid tenant context selection");
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> getMembers(UUID tenantId) {
        validateTenantContext(tenantId);
        
        return membershipRepository.findAllByTenantPublicId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MembershipResponse addMember(UUID tenantId, MembershipRequest request) {
        validateTenantContext(tenantId);

        if (!authorizationService.canManageMembers()) {
            throw new MembershipDeniedException("Only OWNER and ADMIN can manage members");
        }

        if (request.getUserPublicId() == null) {
            throw new InvalidRequestException("User public ID is required");
        }

        Tenant tenant = tenantRepository.findByPublicId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        User user = userRepository.findByPublicId(request.getUserPublicId())
                .orElseThrow(() -> new InvalidRequestException("User to add not found"));

        if (membershipRepository.findByTenantPublicIdAndUserPublicId(tenantId, request.getUserPublicId()).isPresent()) {
            throw new InvalidRequestException("User is already a member of this tenant");
        }

        TenantMembership membership = new TenantMembership(tenant, user, request.getRole());
        membership = membershipRepository.save(membership);

        return mapToResponse(membership);
    }

    @Transactional
    public MembershipResponse updateMember(UUID tenantId, UUID memberId, MembershipRequest request) {
        validateTenantContext(tenantId);

        if (!authorizationService.canManageMembers()) {
            throw new MembershipDeniedException("Only OWNER and ADMIN can manage members");
        }

        TenantMembership membership = membershipRepository.findByTenantPublicIdAndUserPublicId(tenantId, memberId)
                .orElseThrow(() -> new InvalidRequestException("Membership not found"));

        TenantRole oldRole = membership.getRole();
        TenantRole newRole = request.getRole();

        if (oldRole == TenantRole.OWNER && newRole != TenantRole.OWNER) {
            long ownerCount = membershipRepository.countByTenantPublicIdAndRole(tenantId, TenantRole.OWNER);
            if (ownerCount <= 1) {
                throw new InvalidRequestException("Cannot demote the last OWNER of the tenant");
            }
        }

        membership.updateRole(newRole);
        membership = membershipRepository.save(membership);

        return mapToResponse(membership);
    }

    @Transactional
    public void removeMember(UUID tenantId, UUID memberId) {
        validateTenantContext(tenantId);

        if (!authorizationService.canManageMembers()) {
            throw new MembershipDeniedException("Only OWNER and ADMIN can manage members");
        }

        TenantMembership membership = membershipRepository.findByTenantPublicIdAndUserPublicId(tenantId, memberId)
                .orElseThrow(() -> new InvalidRequestException("Membership not found"));

        if (membership.getRole() == TenantRole.OWNER) {
            long ownerCount = membershipRepository.countByTenantPublicIdAndRole(tenantId, TenantRole.OWNER);
            if (ownerCount <= 1) {
                throw new InvalidRequestException("Cannot remove the last OWNER of the tenant");
            }
        }

        membershipRepository.delete(membership);
    }

    private MembershipResponse mapToResponse(TenantMembership membership) {
        return new MembershipResponse(
                membership.getUser().getPublicId(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }
}
