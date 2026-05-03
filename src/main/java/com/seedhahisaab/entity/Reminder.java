package com.seedhahisaab.entity;

import com.seedhahisaab.domain.ReminderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight follow-up note attached to a financial context.
 *
 * <p><strong>Design rule:</strong> a Reminder is metadata. It does not store
 * amounts, balances, or any computed financial value. Financial truth lives
 * in {@link Transaction} and the personal/project ledgers; reminders only
 * <em>reference</em> those entities by id (or, for counterparties, by name).
 *
 * <p>{@code linkedTransactionId} stores a transaction's
 * {@code root_transaction_id} (not a version-specific id) so reminders survive
 * edits and omits of the underlying transaction.
 *
 * <p>{@code linkedCounterpartyName} is stored trimmed and matched
 * case-insensitively. There is no FK because counterparties are an emergent
 * grouping over personal transactions, not a table.
 *
 * <p>Reminders are scoped to {@code createdByUserId} — even reminders attached
 * to a shared project are private to their creator.
 */
@Entity
@Table(name = "reminders",
        indexes = {
                @Index(name = "idx_rem_owner_due", columnList = "created_by_user_id, due_date"),
                @Index(name = "idx_rem_owner_project", columnList = "created_by_user_id, linked_project_id"),
                @Index(name = "idx_rem_owner_root_txn", columnList = "created_by_user_id, linked_transaction_id"),
                @Index(name = "idx_rem_owner_status", columnList = "created_by_user_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "linked_transaction_id")
    private UUID linkedTransactionId;

    @Column(name = "linked_project_id")
    private UUID linkedProjectId;

    @Column(name = "linked_counterparty_name", length = 255)
    private String linkedCounterpartyName;

    /**
     * Optional link to an installment. The reminder still stores no money;
     * this is a context pointer, mirroring the other linked_* fields.
     */
    @Column(name = "linked_installment_id")
    private UUID linkedInstallmentId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
