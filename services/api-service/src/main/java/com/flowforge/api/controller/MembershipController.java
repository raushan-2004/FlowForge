package com.flowforge.api.controller;

import com.flowforge.api.dto.MembershipRequest;
import com.flowforge.api.dto.MembershipResponse;
import com.flowforge.api.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/members")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public ResponseEntity<List<MembershipResponse>> getMembers(
            @PathVariable("tenantId") UUID tenantId) {
        List<MembershipResponse> response = membershipService.getMembers(tenantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MembershipResponse> addMember(
            @PathVariable("tenantId") UUID tenantId,
            @Valid @RequestBody MembershipRequest request) {
        MembershipResponse response = membershipService.addMember(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MembershipResponse> updateMember(
            @PathVariable("tenantId") UUID tenantId,
            @PathVariable("memberId") UUID memberId,
            @Valid @RequestBody MembershipRequest request) {
        MembershipResponse response = membershipService.updateMember(tenantId, memberId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable("tenantId") UUID tenantId,
            @PathVariable("memberId") UUID memberId) {
        membershipService.removeMember(tenantId, memberId);
        return ResponseEntity.noContent().build();
    }
}
