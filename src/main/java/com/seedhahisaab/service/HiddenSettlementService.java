package com.seedhahisaab.service;

import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.dto.hidden.HiddenSettlementResponse;
import com.seedhahisaab.dto.hidden.HiddenSettlementResponse.OfficialPartnerGroup;
import com.seedhahisaab.dto.hidden.HiddenSettlementResponse.Row;
import com.seedhahisaab.dto.summary.PartnerSettlementResponse;
import com.seedhahisaab.entity.HiddenPartnerAgreement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Derives the private redistribution view of a project's profit allocation.
 *
 * <p>This service is <em>read-only</em> and never writes to any other domain.
 * It pulls the official partner settlement (which itself is derived from
 * project transactions) and splits each official partner's profit share
 * according to the active hidden agreements stamped against that partner by
 * the current user.
 *
 * <p><strong>Math (single level only):</strong>
 * <pre>
 *   effectiveProjectShare = officialShare    * hiddenShare / 100
 *   expectedProfit        = officialProfit   * hiddenShare / 100
 *   withdrawn             = 0                                   (v1)
 *   pendingSettlement     = expectedProfit - withdrawn
 * </pre>
 *
 * <p>An implicit "self retained" row is appended per official partner showing
 * the slice the official partner has not redistributed
 * ({@code 100 - sum(active hidden %)}).
 *
 * <p>Negative project profit produces negative {@code expectedProfit} —
 * surfaced honestly as an expected loss share. We never clamp to zero,
 * because hiding losses would be a worse lie than showing them.
 *
 * <p>Visibility: every row carries
 * {@link FinancialVisibilityScope#PRIVATE}. The whole payload is only
 * reachable via routes that already check creator ownership.
 */
@Service
public class HiddenSettlementService {

    private final PartnerSettlementService partnerSettlementService;
    private final HiddenPartnerAgreementService hiddenAgreementService;

    public HiddenSettlementService(PartnerSettlementService partnerSettlementService,
                                   HiddenPartnerAgreementService hiddenAgreementService) {
        this.partnerSettlementService = partnerSettlementService;
        this.hiddenAgreementService = hiddenAgreementService;
    }

    public HiddenSettlementResponse getForProject(UUID projectId, UUID userId) {
        // Both calls already check project access; agreement service
        // additionally enforces creator-scoped reads.
        List<PartnerSettlementResponse> official =
                partnerSettlementService.getSettlement(projectId, userId);
        List<HiddenPartnerAgreement> agreements =
                hiddenAgreementService.requireActiveForProject(projectId, userId);

        Map<UUID, List<HiddenPartnerAgreement>> byPartner = new LinkedHashMap<>();
        for (HiddenPartnerAgreement h : agreements) {
            byPartner.computeIfAbsent(h.getOfficialPartnerId(), k -> new ArrayList<>()).add(h);
        }

        List<OfficialPartnerGroup> groups = new ArrayList<>();
        for (PartnerSettlementResponse o : official) {
            List<HiddenPartnerAgreement> rowsForThis = byPartner
                    .getOrDefault(o.getPartnerId(), List.of());
            // Skip official partners with no hidden agreements — the private
            // view is meant to highlight redistribution. The official view
            // already shows everyone.
            if (rowsForThis.isEmpty()) continue;
            groups.add(buildGroup(o, rowsForThis));
        }

        return HiddenSettlementResponse.builder()
                .groups(groups)
                .visibilityScope(FinancialVisibilityScope.PRIVATE)
                .build();
    }

    private OfficialPartnerGroup buildGroup(PartnerSettlementResponse official,
                                            List<HiddenPartnerAgreement> agreements) {
        BigDecimal officialShare = nz(official.getSharePercentage());
        BigDecimal officialProfit = nz(official.getPartnerProfitShare());

        BigDecimal totalHidden = BigDecimal.ZERO;
        List<Row> rows = new ArrayList<>();
        for (HiddenPartnerAgreement h : agreements) {
            BigDecimal hiddenPct = nz(h.getSharePercentage());
            totalHidden = totalHidden.add(hiddenPct);

            BigDecimal effective = officialShare
                    .multiply(hiddenPct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal expected = officialProfit
                    .multiply(hiddenPct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal withdrawn = BigDecimal.ZERO; // v1: derived only
            BigDecimal pending = expected.subtract(withdrawn);

            rows.add(Row.builder()
                    .agreementId(h.getId())
                    .hiddenPartnerName(h.getHiddenPartnerName())
                    .selfRetained(false)
                    .sharePercentage(hiddenPct)
                    .effectiveProjectShare(effective)
                    .expectedProfit(expected)
                    .withdrawn(withdrawn)
                    .pendingSettlement(pending)
                    .visibilityScope(FinancialVisibilityScope.PRIVATE)
                    .build());
        }

        BigDecimal selfPct = BigDecimal.valueOf(100).subtract(totalHidden);
        // Defensive: negative is impossible because the cap is enforced on
        // create/update, but show 0 instead of a negative number in case
        // historical data drifts in.
        if (selfPct.signum() < 0) selfPct = BigDecimal.ZERO;
        BigDecimal selfEffective = officialShare
                .multiply(selfPct)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal selfExpected = officialProfit
                .multiply(selfPct)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        rows.add(Row.builder()
                .agreementId(null)
                .hiddenPartnerName(official.getPartnerName() + " (kept)")
                .selfRetained(true)
                .sharePercentage(selfPct)
                .effectiveProjectShare(selfEffective)
                .expectedProfit(selfExpected)
                .withdrawn(BigDecimal.ZERO)
                .pendingSettlement(selfExpected)
                .visibilityScope(FinancialVisibilityScope.PRIVATE)
                .build());

        return OfficialPartnerGroup.builder()
                .officialPartnerId(official.getPartnerId())
                .officialPartnerName(official.getPartnerName())
                .officialSharePercentage(officialShare)
                .officialProfitShare(officialProfit)
                .totalHiddenSharePercentage(totalHidden)
                .selfRetainedSharePercentage(selfPct)
                .rows(rows)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
