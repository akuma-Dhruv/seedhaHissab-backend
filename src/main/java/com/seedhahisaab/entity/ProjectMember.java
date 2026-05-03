package com.seedhahisaab.entity;

import com.seedhahisaab.domain.ProjectMemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One person's membership in a project. The presence of an active row
 * (i.e. {@code archivedAt IS NULL}) is the entire authorization signal —
 * {@link com.seedhahisaab.service.ProjectAccessService} is the only place
 * that should consult it.
 *
 * <p><strong>Invariants:</strong>
 * <ul>
 *   <li>(project_id, user_id) is unique. Re-inviting an archived member
 *       restores the existing row instead of creating a duplicate.</li>
 *   <li>Archived rows are kept forever for actor-attribution on the
 *       activity timeline.</li>
 *   <li>A project always has at least one active OWNER. The service
 *       layer enforces this on role updates and archives.</li>
 * </ul>
 */
@Entity
@Table(name = "project_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pm_project_user", columnNames = {"project_id", "user_id"})
}, indexes = {
        @Index(name = "idx_pm_project", columnList = "project_id"),
        @Index(name = "idx_pm_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectMemberRole role;

    /** Null for backfilled creators and the very first OWNER on project creation. */
    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Soft-archive marker. Setting this revokes access on the next request. */
    @Column(name = "archived_at")
    private Instant archivedAt;

    public boolean isActive() {
        return archivedAt == null;
    }
}
