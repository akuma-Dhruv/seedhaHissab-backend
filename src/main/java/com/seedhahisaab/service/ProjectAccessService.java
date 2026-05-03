package com.seedhahisaab.service;

import com.seedhahisaab.domain.ProjectMemberRole;
import com.seedhahisaab.entity.Project;
import com.seedhahisaab.entity.ProjectMember;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.ProjectMemberRepository;
import com.seedhahisaab.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The ONE authority for project authorization.
 *
 * <p>No controller, service, or repository should independently inspect
 * {@code projects.created_by}, role columns, or {@code archived_at} to
 * decide access. They call one of the {@code requireXxx} helpers here and
 * either receive a {@link ProjectAccess} record or get an exception.
 *
 * <p>Why centralize so aggressively? Three reasons:
 * <ol>
 *   <li>Future approval workflows, audit logs, and notification hooks all
 *       want a single chokepoint for "who touched what."</li>
 *   <li>Capability semantics (especially the OWNER/EDITOR vs. ACCOUNTANT
 *       split) live in one place — easier to reason about and test.</li>
 *   <li>Bug surface shrinks: any access bug is a bug in this file or a
 *       service that bypassed it.</li>
 * </ol>
 *
 * <p>{@link ProjectMemberRole} carries an internal {@code rank} so the
 * helpers below stay free of giant switch statements. The capability
 * predicates ({@code canCreateTransaction}, {@code canEditTransaction},
 * {@code canManageMembers}) live on the enum — they're the contract; the
 * rank is for "at least as privileged as" comparisons only.
 */
@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(ProjectRepository projectRepository,
                                ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * Bundle of project + active membership returned by the require* helpers.
     * Carrying both means callers never re-query the project row.
     */
    public record ProjectAccess(Project project, ProjectMember member) {
        public ProjectMemberRole role() {
            return member.getRole();
        }
    }

    // -------------------------------------------------------------------------
    // Generic role-rank helpers
    // -------------------------------------------------------------------------

    /** Any active member (OWNER, EDITOR, ACCOUNTANT, VIEWER). */
    public ProjectAccess requireProjectViewer(UUID projectId, UUID userId) {
        return resolve(projectId, userId, ProjectMemberRole.VIEWER);
    }

    /**
     * "Editor or above" by rank: OWNER, EDITOR, ACCOUNTANT all pass. The
     * finer-grained transaction split (create vs. edit) is enforced via the
     * dedicated capability helpers below — services should prefer those.
     */
    public ProjectAccess requireProjectEditor(UUID projectId, UUID userId) {
        return resolve(projectId, userId, ProjectMemberRole.ACCOUNTANT);
    }

    public ProjectAccess requireProjectOwner(UUID projectId, UUID userId) {
        return resolve(projectId, userId, ProjectMemberRole.OWNER);
    }

    // -------------------------------------------------------------------------
    // Capability-specific helpers (preferred by services)
    // -------------------------------------------------------------------------

    /** OWNER, EDITOR, ACCOUNTANT — anyone who can record a new transaction. */
    public ProjectAccess requireCanCreateTransaction(UUID projectId, UUID userId) {
        ProjectAccess access = requireProjectViewer(projectId, userId);
        if (!access.role().canCreateTransaction()) {
            throw ApiException.forbidden("View-only access. You cannot record transactions on this project.");
        }
        return access;
    }

    /** OWNER, EDITOR — ACCOUNTANT cannot edit existing rows. */
    public ProjectAccess requireCanEditTransaction(UUID projectId, UUID userId) {
        ProjectAccess access = requireProjectViewer(projectId, userId);
        if (!access.role().canEditTransaction()) {
            throw ApiException.forbidden("Only owners and editors can edit transactions on this project.");
        }
        return access;
    }

    /** OWNER only — manage members, change roles, archive/restore. */
    public ProjectAccess requireCanManageMembers(UUID projectId, UUID userId) {
        ProjectAccess access = requireProjectViewer(projectId, userId);
        if (!access.role().canManageMembers()) {
            throw ApiException.forbidden("Only the project owner can manage members.");
        }
        return access;
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private ProjectAccess resolve(UUID projectId, UUID userId, ProjectMemberRole minimum) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project not found"));

        ProjectMember member = projectMemberRepository
                .findActiveByProjectAndUser(projectId, userId)
                .orElseThrow(() -> ApiException.forbidden("Access denied"));

        if (!member.getRole().atLeast(minimum)) {
            throw ApiException.forbidden("Access denied");
        }

        return new ProjectAccess(project, member);
    }
}
