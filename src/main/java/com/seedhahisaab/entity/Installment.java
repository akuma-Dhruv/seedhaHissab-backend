package com.seedhahisaab.entity;

import com.seedhahisaab.domain.InstallmentManualStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An expected receivable — a single installment in a customer's payment
 * schedule for a project.
 *
 * <p><strong>Design rule:</strong> the row stores the <em>promise</em>
 * (expected amount + due date) and a manual lifecycle flag. It never stores
 * the running received total, the remaining balance, or the user-facing
 * status. Those are computed at read-time by {@code InstallmentService} from
 * the linked INCOME transactions, so the engine cannot drift out of sync with
 * the underlying ledger when transactions are edited or omitted.
 */
@Entity
@Table(name = "installments",
        indexes = {
                @Index(name = "idx_inst_project_due", columnList = "project_id, due_date"),
                @Index(name = "idx_inst_customer", columnList = "customer_id"),
                @Index(name = "idx_inst_owner", columnList = "created_by_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1024)
    private String description;

    /** The promise. Never the truth — actual collected money lives on transactions. */
    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Persisted lifecycle state. PENDING by default; CANCELLED when explicitly cancelled. */
    @Enumerated(EnumType.STRING)
    @Column(name = "manual_status", nullable = false, length = 32)
    @Builder.Default
    private InstallmentManualStatus manualStatus = InstallmentManualStatus.PENDING;

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
