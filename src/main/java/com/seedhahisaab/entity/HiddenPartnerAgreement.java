package com.seedhahisaab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A private redistribution of one official partner's slice. Records the fact
 * that an official partner has informally promised a percentage of their own
 * project share to an off-the-books stakeholder (silent investor, family
 * member, sleeping partner, etc.).
 *
 * <p><strong>Critical contracts:</strong>
 * <ul>
 *   <li>{@code sharePercentage} is a percentage of the official partner's
 *       slice, NOT a percentage of the project. A 25% agreement on a 40%
 *       official partner equals 10% effective project ownership.</li>
 *   <li>Visibility is creator-only. The row never leaks into project
 *       summaries, partner settlement endpoints, or transaction listings.</li>
 *   <li>No stored balances. Expected payouts are derived live from
 *       {@code PartnerSettlementService} output.</li>
 *   <li>Single-level only. There is no FK from a hidden agreement to another
 *       hidden agreement, and the service rejects any such future attempt.</li>
 *   <li>Soft archive only. Hard delete is intentionally disabled — the
 *       agreement is the only audit trail of who got what.</li>
 * </ul>
 */
@Entity
@Table(name = "hidden_partner_agreements", indexes = {
        @Index(name = "idx_hpa_project", columnList = "project_id"),
        @Index(name = "idx_hpa_official_partner", columnList = "official_partner_id"),
        @Index(name = "idx_hpa_owner_archived", columnList = "created_by_user_id, archived_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiddenPartnerAgreement {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** The official partner whose slice is being privately redistributed. */
    @Column(name = "official_partner_id", nullable = false)
    private UUID officialPartnerId;

    @Column(name = "hidden_partner_name", nullable = false, length = 255)
    private String hiddenPartnerName;

    /** Reserved for future multi-user linkage. Always null in v1. */
    @Column(name = "hidden_partner_user_id")
    private UUID hiddenPartnerUserId;

    /** Percentage of the official partner's slice, not of the project. */
    @Column(name = "share_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercentage;

    @Column(name = "notes", length = 2048)
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** NULL when the agreement is active. Soft-archived rows are excluded
     *  from active calculations and from the 100% cap on the same official
     *  partner, but remain readable for history. */
    @Column(name = "archived_at")
    private Instant archivedAt;

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
