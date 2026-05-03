package com.seedhahisaab.service;

import com.seedhahisaab.domain.ProjectMemberRole;
import com.seedhahisaab.dto.member.ProjectMemberInviteRequest;
import com.seedhahisaab.dto.member.ProjectMemberResponse;
import com.seedhahisaab.dto.member.ProjectMemberRoleUpdateRequest;
import com.seedhahisaab.entity.Project;
import com.seedhahisaab.entity.ProjectMember;
import com.seedhahisaab.entity.User;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.ProjectMemberRepository;
import com.seedhahisaab.repository.ProjectRepository;
import com.seedhahisaab.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Member management. Every public method is gated by
 * {@link ProjectAccessService} — there is zero direct {@code createdBy} /
 * role inspection in this class.
 *
 * <p>The last-OWNER guard runs under a {@code SELECT FOR UPDATE} on the
 * project row. Two concurrent "downgrade the last OWNER" requests will
 * therefore serialize — the first commits, the second sees the updated
 * count and rejects.
 */
@Service
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;
    private final EntityManager entityManager;

    public ProjectMemberService(ProjectMemberRepository memberRepository,
                                ProjectRepository projectRepository,
                                UserRepository userRepository,
                                ProjectAccessService projectAccessService,
                                EntityManager entityManager) {
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public List<ProjectMemberResponse> list(UUID projectId, UUID callerUserId, boolean includeArchived) {
        projectAccessService.requireProjectViewer(projectId, callerUserId);
        List<ProjectMember> members = includeArchived
                ? memberRepository.findAllByProject(projectId)
                : memberRepository.findActiveByProject(projectId);
        return hydrate(members);
    }

    // -------------------------------------------------------------------------
    // Invite (or restore-on-reinvite)
    // -------------------------------------------------------------------------

    /**
     * Invite a user by email. v1: the user must already exist — we do not
     * create stub accounts or send invitation emails.
     *
     * <p>Re-inviting an archived member restores their row (and updates
     * the role) instead of creating a duplicate. Re-inviting an already-
     * active member returns 409.
     */
    @Transactional
    public ProjectMemberResponse invite(UUID projectId, ProjectMemberInviteRequest req, UUID callerUserId) {
        projectAccessService.requireCanManageMembers(projectId, callerUserId);

        if (req.getRole() == null) throw ApiException.badRequest("Role is required");

        String normalizedEmail = req.getEmail().trim().toLowerCase();
        User invitee = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmail(req.getEmail().trim()))
                .orElseThrow(() -> ApiException.notFound(
                        "No user found with that email. Ask them to sign up first."));

        Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(projectId, invitee.getId());
        if (existing.isPresent()) {
            ProjectMember m = existing.get();
            if (m.isActive()) {
                throw ApiException.conflict("User is already a member of this project.");
            }
            // Restore archived membership with the (possibly new) role.
            m.setArchivedAt(null);
            m.setRole(req.getRole());
            m.setInvitedByUserId(callerUserId);
            return hydrateOne(memberRepository.save(m));
        }

        ProjectMember member = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .userId(invitee.getId())
                .role(req.getRole())
                .invitedByUserId(callerUserId)
                .build();
        return hydrateOne(memberRepository.save(member));
    }

    // -------------------------------------------------------------------------
    // Role update
    // -------------------------------------------------------------------------

    @Transactional
    public ProjectMemberResponse updateRole(UUID projectId, UUID memberId,
                                             ProjectMemberRoleUpdateRequest req, UUID callerUserId) {
        projectAccessService.requireCanManageMembers(projectId, callerUserId);
        if (req.getRole() == null) throw ApiException.badRequest("Role is required");

        // SELECT FOR UPDATE on the project row serializes concurrent
        // last-OWNER mutations on the same project.
        lockProject(projectId);

        ProjectMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));

        if (!member.getProjectId().equals(projectId)) {
            throw ApiException.notFound("Member not found");
        }
        if (!member.isActive()) {
            throw ApiException.badRequest("Cannot change role of an archived member. Restore them first.");
        }

        ProjectMemberRole oldRole = member.getRole();
        ProjectMemberRole newRole = req.getRole();
        if (oldRole == newRole) return hydrateOne(member);

        // Last-OWNER guard. If we're downgrading an OWNER, ensure another
        // active OWNER exists.
        if (oldRole == ProjectMemberRole.OWNER && newRole != ProjectMemberRole.OWNER) {
            long activeOwners = memberRepository.countActiveOwners(projectId);
            if (activeOwners <= 1) {
                throw ApiException.badRequest(
                        "A project must have at least one owner. Promote another member to owner first.");
            }
        }

        member.setRole(newRole);
        return hydrateOne(memberRepository.save(member));
    }

    // -------------------------------------------------------------------------
    // Archive / restore
    // -------------------------------------------------------------------------

    @Transactional
    public ProjectMemberResponse archive(UUID projectId, UUID memberId, UUID callerUserId) {
        projectAccessService.requireCanManageMembers(projectId, callerUserId);
        lockProject(projectId);

        ProjectMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));
        if (!member.getProjectId().equals(projectId)) {
            throw ApiException.notFound("Member not found");
        }
        if (!member.isActive()) {
            return hydrateOne(member);
        }

        if (member.getRole() == ProjectMemberRole.OWNER) {
            long activeOwners = memberRepository.countActiveOwners(projectId);
            if (activeOwners <= 1) {
                throw ApiException.badRequest(
                        "Cannot archive the last owner. Promote another member to owner first.");
            }
        }

        member.setArchivedAt(Instant.now());
        return hydrateOne(memberRepository.save(member));
    }

    @Transactional
    public ProjectMemberResponse restore(UUID projectId, UUID memberId, UUID callerUserId) {
        projectAccessService.requireCanManageMembers(projectId, callerUserId);

        ProjectMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));
        if (!member.getProjectId().equals(projectId)) {
            throw ApiException.notFound("Member not found");
        }
        if (member.isActive()) {
            return hydrateOne(member);
        }
        member.setArchivedAt(null);
        return hydrateOne(memberRepository.save(member));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void lockProject(UUID projectId) {
        Project project = entityManager.find(Project.class, projectId, LockModeType.PESSIMISTIC_WRITE);
        if (project == null) {
            throw ApiException.notFound("Project not found");
        }
    }

    private List<ProjectMemberResponse> hydrate(List<ProjectMember> members) {
        if (members.isEmpty()) return List.of();
        Set<UUID> userIds = members.stream().map(ProjectMember::getUserId).collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return members.stream()
                .map(m -> ProjectMemberResponse.from(m, users.get(m.getUserId())))
                .collect(Collectors.toList());
    }

    private ProjectMemberResponse hydrateOne(ProjectMember member) {
        User user = userRepository.findById(member.getUserId()).orElse(null);
        return ProjectMemberResponse.from(member, user);
    }
}
