package com.seedhahisaab.dto.hidden;

import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.entity.HiddenPartnerAgreement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The agreement row plus a couple of denormalised display fields:
 * {@code officialPartnerName} (so the FE doesn't need a separate fetch) and
 * {@code archived} (true when {@code archivedAt} is non-null). Carries a
 * {@link FinancialVisibilityScope} marker stamped {@code PRIVATE} on every
 * response — clients use this to drive the "Private/Internal Only" banner.
 */
@Data
@Builder
@AllArgsConstructor
public class HiddenPartnerAgreementResponse {

    private UUID id;
    private UUID projectId;
    private UUID officialPartnerId;
    private String officialPartnerName;
    private String hiddenPartnerName;
    private UUID hiddenPartnerUserId;
    private BigDecimal sharePercentage;
    private String notes;
    private boolean archived;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;
    private FinancialVisibilityScope visibilityScope;

    public static HiddenPartnerAgreementResponse from(
            HiddenPartnerAgreement h, String officialPartnerName) {
        return HiddenPartnerAgreementResponse.builder()
                .id(h.getId())
                .projectId(h.getProjectId())
                .officialPartnerId(h.getOfficialPartnerId())
                .officialPartnerName(officialPartnerName)
                .hiddenPartnerName(h.getHiddenPartnerName())
                .hiddenPartnerUserId(h.getHiddenPartnerUserId())
                .sharePercentage(h.getSharePercentage())
                .notes(h.getNotes())
                .archived(h.getArchivedAt() != null)
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .archivedAt(h.getArchivedAt())
                .visibilityScope(FinancialVisibilityScope.PRIVATE)
                .build();
    }
}
