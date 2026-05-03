package com.seedhahisaab.dto.hidden;

import com.seedhahisaab.domain.FinancialVisibilityScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Derived private settlement view. Returned by
 * {@code GET /projects/{projectId}/hidden-settlements}. Three nested shapes:
 *
 * <ul>
 *   <li>{@link Row} — one line per active hidden agreement, plus an implicit
 *       "self" row per official partner showing whatever slice the partner
 *       hasn't redistributed.</li>
 *   <li>{@link OfficialPartnerGroup} — rows grouped under their official
 *       partner, with the group totals coming from the official settlement
 *       engine (NOT recomputed here).</li>
 * </ul>
 *
 * <p>Every {@code Row} carries {@link FinancialVisibilityScope#PRIVATE} so
 * the FE can mark anything sourced here with the private-only banner.
 */
@Data
@Builder
@AllArgsConstructor
public class HiddenSettlementResponse {

    private List<OfficialPartnerGroup> groups;
    /** Visibility marker for the entire payload. Always PRIVATE. */
    private FinancialVisibilityScope visibilityScope;

    @Data
    @Builder
    @AllArgsConstructor
    public static class OfficialPartnerGroup {
        private UUID officialPartnerId;
        private String officialPartnerName;
        /** Official partner's project share, copied from PartnerSettlementService. */
        private BigDecimal officialSharePercentage;
        /** Official partner's profit share for the project (already derived). */
        private BigDecimal officialProfitShare;
        /** Sum of active hidden share %s on this partner. */
        private BigDecimal totalHiddenSharePercentage;
        /** {@code 100 - totalHiddenSharePercentage}. What the official partner keeps. */
        private BigDecimal selfRetainedSharePercentage;
        /** Hidden agreement rows + a synthetic "self" row, in the same order. */
        private List<Row> rows;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class Row {
        /** Null for the synthetic "self" row that represents the official partner's retained slice. */
        private UUID agreementId;
        private String hiddenPartnerName;
        /** True for the synthetic "self" row. */
        private boolean selfRetained;
        /** Percentage of the official partner's slice (e.g. 25). */
        private BigDecimal sharePercentage;
        /** Effective % of total project (officialShare * sharePercentage / 100). */
        private BigDecimal effectiveProjectShare;
        /** Expected private profit (officialProfitShare * sharePercentage / 100). Negative if project at a loss. */
        private BigDecimal expectedProfit;
        /** Always 0 in v1 — derived layer, no withdrawal tracking yet. */
        private BigDecimal withdrawn;
        /** {@code expectedProfit - withdrawn}. */
        private BigDecimal pendingSettlement;
        private FinancialVisibilityScope visibilityScope;
    }
}
