package com.flowforge.api.service;

import com.flowforge.api.dto.ProjectRequest;
import com.flowforge.api.dto.ProjectResponse;
import com.flowforge.api.exception.InvalidRequestException;
import com.flowforge.api.exception.MembershipDeniedException;
import com.flowforge.api.exception.ResourceNotFoundException;
import com.flowforge.api.exception.TenantNotFoundException;
import com.flowforge.api.model.Project;
import com.flowforge.api.model.ProjectStatus;
import com.flowforge.api.model.Tenant;
import com.flowforge.api.repository.ProjectRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final TenantAuthorizationService authorizationService;
    private final com.flowforge.api.shared.identity.PublicIdGenerator publicIdGenerator;

    public ProjectService(
            ProjectRepository projectRepository,
            TenantRepository tenantRepository,
            TenantAuthorizationService authorizationService,
            com.flowforge.api.shared.identity.PublicIdGenerator publicIdGenerator) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.authorizationService = authorizationService;
        this.publicIdGenerator = publicIdGenerator;
    }

    private TenantSecurityContext getActiveTenantContext() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            throw new MembershipDeniedException("Active tenant context is required");
        }
        return context;
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canCreateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER roles can create projects");
        }

        Tenant tenant = tenantRepository.findByPublicId(context.getTenantPublicId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        String name = request.getName().trim();
        if (projectRepository.findByTenantAndNameIgnoreCase(tenant, name).isPresent()) {
            throw new InvalidRequestException("Project name is already taken within the selected tenant");
        }

        Project project = new Project(
                publicIdGenerator.generate(),
                tenant,
                name,
                ProjectStatus.ACTIVE,
                context.getUserPublicId()
        );

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        TenantSecurityContext context = getActiveTenantContext();
        
        if (context.isAutomation()) {
            return projectRepository.findByPublicIdAndTenantPublicId(context.getProjectPublicId(), context.getTenantPublicId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        
        return projectRepository.findAllByTenantPublicId(context.getTenantPublicId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (context.isAutomation() && !context.getProjectPublicId().equals(projectId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        Project project = projectRepository.findByPublicIdAndTenantPublicId(projectId, context.getTenantPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canUpdateProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER roles can update projects");
        }

        Project project = projectRepository.findByPublicIdAndTenantPublicId(projectId, context.getTenantPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(project.getName())) {
                if (projectRepository.findByTenantAndNameIgnoreCase(project.getTenant(), newName).isPresent()) {
                    throw new InvalidRequestException("Project name is already taken within the selected tenant");
                }
                project.rename(newName, context.getUserPublicId());
            }
        }

        if (request.getStatus() != null) {
            try {
                ProjectStatus newStatus = ProjectStatus.valueOf(request.getStatus().trim().toUpperCase());
                if (newStatus != project.getStatus()) {
                    switch (newStatus) {
                        case ACTIVE -> project.activate(context.getUserPublicId());
                        case SUSPENDED -> project.suspend(context.getUserPublicId());
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Invalid project status: " + request.getStatus());
            }
        }

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (!authorizationService.canArchiveProjects()) {
            throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER roles can archive projects");
        }

        Project project = projectRepository.findByPublicIdAndTenantPublicId(projectId, context.getTenantPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Archive instead of delete: use existing status model (SUSPENDED represents archived)
        project.suspend(context.getUserPublicId());
        projectRepository.save(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(
                project.getPublicId(),
                project.getName(),
                project.getStatus(),
                project.getTenant().getPublicId(),
                project.getCreatedBy(),
                project.getUpdatedBy(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
