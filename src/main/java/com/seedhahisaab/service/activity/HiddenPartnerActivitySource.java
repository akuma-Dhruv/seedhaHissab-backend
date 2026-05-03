package com.seedhahisaab.service.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.entity.HiddenPartnerAgreement;
import com.seedhahisaab.repository.HiddenPartnerAgreementRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emits {@link ActivityType#HIDDEN_PARTNER_CREATED}, {@code UPDATED}, and
 * {@code ARCHIVED} events for hidden agreements <strong>visible to the
 * caller</strong>.
 *
 * <p><strong>Privacy invariant — defence in depth:</strong> the underlying
 * repository method {@code findAllForProject(projectId, userId)} is itself
 * creator-scoped, so even if a future bug let a non-creator hit the project
 * activity route the data layer still returns nothing for them. Every row
 * emitted here is also stamped {@link FinancialVisibilityScope#PRIVATE} so
 * the FE renders the lock indicator regardless of any client-side filter.
 *
 * <p>{@link ActivityType#HIDDEN_PARTNER_RESTORED} is intentionally never
 * emitted in v1 — see {@code ActivityType} javadoc.
 */
@Component
public class HiddenPartnerActivitySource {

    private static final Duration UPDATE_TOLERANCE = Duration.ofSeconds(2);

    private final HiddenPartnerAgreementRepository repo;
    private final ActivityNarrator narrator;

    public HiddenPartnerActivitySource(HiddenPartnerAgreementRepository repo,
                                       ActivityNarrator narrator) {
        this.repo = repo;
        this.narrator = narrator;
    }

    public List<ActivityItemDTO> forProject(UUID projectId, UUID callerId) {
        // Every read here is creator-scoped at the repository layer.
        List<HiddenPartnerAgreement> rows = repo.findAllForProject(projectId, callerId);
        List<ActivityItemDTO> out = new ArrayList<>();
        for (HiddenPartnerAgreement h : rows) {
            out.add(buildCreated(h));
            boolean archived = h.getArchivedAt() != null;
            if (archived) {
                out.add(buildArchived(h));
            } else if (h.getUpdatedAt() != null && h.getCreatedAt() != null
                    && Duration.between(h.getCreatedAt(), h.getUpdatedAt()).compareTo(UPDATE_TOLERANCE) > 0) {
                out.add(buildUpdated(h));
            }
        }
        return out;
    }

    public void narrate(List<ActivityItemDTO> items, UUID callerId, Map<UUID, String> nameMap) {
        for (ActivityItemDTO i : items) {
            String actor = narrator.formatActor(i.getActorUserId(), callerId, nameMap);
            String verb = (String) i.getExtraData().get("__verb");
            String ctx = (String) i.getExtraData().get("__context");
            i.setTitle(actor + " " + verb);
            i.setSubtitle(ctx);
            i.getExtraData().remove("__verb");
            i.getExtraData().remove("__context");
        }
    }

    // -- builders -------------------------------------------------------------

    private ActivityItemDTO buildCreated(HiddenPartnerAgreement h) {
        Map<String, Object> extras = baseExtras(h);
        extras.put("__verb", "added private partner " + h.getHiddenPartnerName()
                + " (" + h.getSharePercentage().stripTrailingZeros().toPlainString() + "% of partner slice)");
        extras.put("__context", "Private — visible only to you");
        return narrator.base(ActivityType.HIDDEN_PARTNER_CREATED, h.getId(), h.getCreatedAt(),
                        FinancialVisibilityScope.PRIVATE)
                .actorUserId(h.getCreatedByUserId())
                .linkedEntityType("HIDDEN_PARTNER")
                .linkedEntityId(h.getId())
                .badge("PRIVATE")
                .extraData(extras)
                .build();
    }

    private ActivityItemDTO buildUpdated(HiddenPartnerAgreement h) {
        Map<String, Object> extras = baseExtras(h);
        extras.put("__verb", "updated private partner " + h.getHiddenPartnerName());
        extras.put("__context", "Now " + h.getSharePercentage().stripTrailingZeros().toPlainString()
                + "% of partner slice · Private");
        return narrator.base(ActivityType.HIDDEN_PARTNER_UPDATED, h.getId(), h.getUpdatedAt(),
                        FinancialVisibilityScope.PRIVATE)
                .actorUserId(h.getCreatedByUserId())
                .linkedEntityType("HIDDEN_PARTNER")
                .linkedEntityId(h.getId())
                .badge("PRIVATE")
                .extraData(extras)
                .build();
    }

    private ActivityItemDTO buildArchived(HiddenPartnerAgreement h) {
        Map<String, Object> extras = baseExtras(h);
        extras.put("__verb", "archived private partner " + h.getHiddenPartnerName());
        extras.put("__context", "Private — visible only to you");
        return narrator.base(ActivityType.HIDDEN_PARTNER_ARCHIVED, h.getId(), h.getArchivedAt(),
                        FinancialVisibilityScope.PRIVATE)
                .actorUserId(h.getCreatedByUserId())
                .linkedEntityType("HIDDEN_PARTNER")
                .linkedEntityId(h.getId())
                .badge("PRIVATE")
                .extraData(extras)
                .build();
    }

    private Map<String, Object> baseExtras(HiddenPartnerAgreement h) {
        Map<String, Object> m = new HashMap<>();
        m.put("hiddenPartnerName", h.getHiddenPartnerName());
        m.put("sharePercentage", h.getSharePercentage());
        m.put("officialPartnerId", h.getOfficialPartnerId());
        return m;
    }
}
