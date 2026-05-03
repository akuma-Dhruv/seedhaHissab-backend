package com.seedhahisaab.service;

import com.seedhahisaab.domain.ProjectMemberRole;
import com.seedhahisaab.dto.project.ProjectRequest;
import com.seedhahisaab.dto.project.ProjectResponse;
import com.seedhahisaab.entity.Project;
import com.seedhahisaab.entity.ProjectMember;
import com.seedhahisaab.repository.ProjectMemberRepository;
import com.seedhahisaab.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Project CRUD + the legacy {@code requireProject} compatibility shim.
 *
 * <p>Authorization is delegated to {@link ProjectAccessService}. This class
 * intentionally does NOT inspect roles, {@code createdBy}, or {@code
 * archivedAt} — every check funnels through the access service.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository projectMemberRepository,
                          ProjectAccessService projectAccessService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
    }

    /**
     * Creates a project and atomically inserts the creator as the sole
     * active OWNER member. {@code createdBy} is retained as historical
     * audit metadata only — access is membership-driven from here on.
     */
    @Transactional
    public ProjectResponse create(ProjectRequest req, UUID userId) {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder()
                .id(projectId)
                .name(req.getName())
                .plannedBudget(req.getPlannedBudget())
                .expectedRevenue(req.getExpectedRevenue())
                .createdBy(userId)
                .build();
        projectRepository.save(project);

        ProjectMember owner = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .userId(userId)
                .role(ProjectMemberRole.OWNER)
                .invitedByUserId(null)
                .build();
        projectMemberRepository.save(owner);

        return ProjectResponse.from(project);
    }

    /**
     * Lists every project where the caller has an ACTIVE membership.
     * Invited members see the project the moment their membership is
     * created — the listing is the entry point to collaboration.
     */
    public List<ProjectResponse> getAllForUser(UUID userId) {
        List<UUID> projectIds = projectMemberRepository.findActiveProjectIdsForUser(userId);
        if (projectIds.isEmpty()) return List.of();
        return projectRepository.findAllById(projectIds).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    public ProjectResponse getById(UUID projectId, UUID userId) {
        return ProjectResponse.from(projectAccessService.requireProjectViewer(projectId, userId).project());
    }

    /**
     * Compatibility wrapper. Existing services call this with the same
     * signature they always have — the implementation now goes through
     * {@link ProjectAccessService#requireProjectViewer}, so legacy call
     * sites get membership-based access for free.
     *
     * <p>New code should prefer the typed access helpers directly so the
     * required role is explicit at the call site.
     */
    public Project requireProject(UUID projectId, UUID userId) {
        return projectAccessService.requireProjectViewer(projectId, userId).project();
    }
}
