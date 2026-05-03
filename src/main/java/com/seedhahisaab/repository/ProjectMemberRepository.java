package com.seedhahisaab.repository;

import com.seedhahisaab.domain.ProjectMemberRole;
import com.seedhahisaab.entity.ProjectMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    /** Active membership lookup — the hot path for authorization. */
    @Query("""
            SELECT m FROM ProjectMember m
            WHERE m.projectId = :projectId
              AND m.userId = :userId
              AND m.archivedAt IS NULL
            """)
    Optional<ProjectMember> findActiveByProjectAndUser(@Param("projectId") UUID projectId,
                                                       @Param("userId") UUID userId);

    /** Includes archived rows. Used during invite/restore to prevent duplicates. */
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("""
            SELECT m FROM ProjectMember m
            WHERE m.projectId = :projectId
              AND m.archivedAt IS NULL
            ORDER BY m.createdAt ASC
            """)
    List<ProjectMember> findActiveByProject(@Param("projectId") UUID projectId);

    @Query("""
            SELECT m FROM ProjectMember m
            WHERE m.projectId = :projectId
            ORDER BY m.createdAt ASC
            """)
    List<ProjectMember> findAllByProject(@Param("projectId") UUID projectId);

    @Query("""
            SELECT m.projectId FROM ProjectMember m
            WHERE m.userId = :userId AND m.archivedAt IS NULL
            """)
    List<UUID> findActiveProjectIdsForUser(@Param("userId") UUID userId);

    /**
     * Active OWNER count for a project. Used by the last-OWNER guard on
     * role changes and archives.
     *
     * <p>Callers should already hold a {@code SELECT FOR UPDATE} lock on
     * the project row before invoking this — see
     * {@link com.seedhahisaab.service.ProjectMemberService} for the
     * canonical pattern.
     */
    @Query("""
            SELECT COUNT(m) FROM ProjectMember m
            WHERE m.projectId = :projectId
              AND m.role = com.seedhahisaab.domain.ProjectMemberRole.OWNER
              AND m.archivedAt IS NULL
            """)
    long countActiveOwners(@Param("projectId") UUID projectId);

    /**
     * Pessimistic-write lock on a member row. Used during role updates and
     * archives that interact with the last-OWNER invariant.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ProjectMember m WHERE m.id = :id")
    Optional<ProjectMember> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByProjectIdAndUserIdAndRoleAndArchivedAtIsNull(UUID projectId, UUID userId, ProjectMemberRole role);
}
