package com.seedhahisaab.repository;

import com.seedhahisaab.domain.ReminderStatus;
import com.seedhahisaab.entity.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndCreatedByUserId(UUID id, UUID userId);

    /**
     * Generic listing — every optional filter is applied conditionally with
     * COALESCE/null-pass-through so a single query can power the
     * /reminders endpoint.
     *
     * <p>{@code statusInclude} restricts to a finite set when non-null;
     * {@code statusExclude} (typically {@code ARCHIVED}) is always honoured.
     */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND (:statusInclude IS NULL OR r.status IN :statusInclude)
              AND (:excludeArchived = FALSE OR r.status <> com.seedhahisaab.domain.ReminderStatus.ARCHIVED)
              AND (:dueAfter  IS NULL OR r.dueDate >= :dueAfter)
              AND (:dueBefore IS NULL OR r.dueDate <= :dueBefore)
              AND (:projectId IS NULL OR r.linkedProjectId = :projectId)
              AND (:counterpartyLower IS NULL OR LOWER(r.linkedCounterpartyName) = :counterpartyLower)
            ORDER BY r.dueDate ASC, r.createdAt ASC
            """)
    Page<Reminder> search(
            @Param("userId") UUID userId,
            @Param("statusInclude") List<ReminderStatus> statusInclude,
            @Param("excludeArchived") boolean excludeArchived,
            @Param("dueAfter") LocalDate dueAfter,
            @Param("dueBefore") LocalDate dueBefore,
            @Param("projectId") UUID projectId,
            @Param("counterpartyLower") String counterpartyLower,
            Pageable pageable);

    /** Today bucket: due today, not completed/archived. */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND r.dueDate = :today
              AND r.status IN (com.seedhahisaab.domain.ReminderStatus.PENDING,
                               com.seedhahisaab.domain.ReminderStatus.SNOOZED)
            ORDER BY r.createdAt ASC
            """)
    List<Reminder> findToday(@Param("userId") UUID userId, @Param("today") LocalDate today);

    /** Overdue bucket: due before today, not completed/archived. */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND r.dueDate < :today
              AND r.status NOT IN (com.seedhahisaab.domain.ReminderStatus.COMPLETED,
                                   com.seedhahisaab.domain.ReminderStatus.ARCHIVED)
            ORDER BY r.dueDate ASC, r.createdAt ASC
            """)
    List<Reminder> findOverdue(@Param("userId") UUID userId, @Param("today") LocalDate today);

    /** Upcoming bucket: due in (today, today + 7], pending or snoozed. */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND r.dueDate > :today
              AND r.dueDate <= :horizon
              AND r.status IN (com.seedhahisaab.domain.ReminderStatus.PENDING,
                               com.seedhahisaab.domain.ReminderStatus.SNOOZED)
            ORDER BY r.dueDate ASC, r.createdAt ASC
            """)
    List<Reminder> findUpcoming(
            @Param("userId") UUID userId,
            @Param("today") LocalDate today,
            @Param("horizon") LocalDate horizon);

    // -- Activity timeline aggregators ---------------------------------------
    //
    // All three queries are scoped by createdByUserId — reminders are
    // creator-private even when attached to a shared project, so the
    // timeline must respect the same boundary.

    /** Project reminders for a user, newest activity (updatedAt) first. */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND r.linkedProjectId = :projectId
            ORDER BY r.updatedAt DESC
            """)
    List<Reminder> findProjectRemindersForActivity(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            org.springframework.data.domain.Pageable pageable);

    /** All reminders for a user, newest activity first. */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
            ORDER BY r.updatedAt DESC
            """)
    List<Reminder> findAllForUserActivity(
            @Param("userId") UUID userId,
            org.springframework.data.domain.Pageable pageable);

    /** Reminders linked to a specific counterparty name (case-insensitive). */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.createdByUserId = :userId
              AND r.linkedCounterpartyName IS NOT NULL
              AND LOWER(TRIM(r.linkedCounterpartyName)) = LOWER(TRIM(CAST(:name AS string)))
            ORDER BY r.updatedAt DESC
            """)
    List<Reminder> findCounterpartyRemindersForActivity(
            @Param("userId") UUID userId,
            @Param("name") String name,
            org.springframework.data.domain.Pageable pageable);
}
