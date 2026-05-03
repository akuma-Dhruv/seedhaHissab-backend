package com.seedhahisaab.entity;

import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.PrePersist;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_txn_root_version", columnNames = {"root_transaction_id", "version"})
        },
        indexes = {
                @Index(name = "idx_txn_project_id", columnList = "project_id"),
                @Index(name = "idx_txn_vendor_project", columnList = "vendor_id, project_id"),
                @Index(name = "idx_txn_paid_by_partner", columnList = "paid_by_partner_id"),
                @Index(name = "idx_txn_root_id", columnList = "root_transaction_id"),
                @Index(name = "idx_txn_owner_user", columnList = "owner_user_id"),
                @Index(name = "idx_txn_owner_counterparty", columnList = "owner_user_id, counterparty_name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "root_transaction_id", nullable = false)
    private UUID rootTransactionId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "paid_by_partner_id")
    private UUID paidByPartnerId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "counterparty_name")
    private String counterpartyName;

    @Column(name = "counterparty_user_id")
    private UUID counterpartyUserId;

    @Column(name = "purpose")
    private String purpose;

    /**
     * Optional link to an {@link Installment}. Set ONLY for {@code type=INCOME}
     * and only when the installment belongs to the same project. Validated in
     * {@code TransactionService}; receivable derivation in
     * {@code InstallmentService} sums latest-version, ACTIVE, INCOME rows
     * matching this column.
     */
    @Column(name = "linked_installment_id")
    private UUID linkedInstallmentId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.ACTIVE;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
