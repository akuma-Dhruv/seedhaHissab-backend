package com.seedhahisaab.service.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.domain.InstallmentManualStatus;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.entity.Installment;
import com.seedhahisaab.repository.InstallmentRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emits CREATED, UPDATED, and CANCELLED events for installments.
 *
 * <p>UPDATE detection without a history table uses a 2-second tolerance on
 * the {@code updatedAt} - {@code createdAt} delta to avoid spurious UPDATED
 * events from the {@code @PrePersist} + {@code @PreUpdate} timing on the
 * initial insert. This is intentionally simple — the real solution arrives
 * the day we add a versioned activity table.
 */
@Component
public class InstallmentActivitySource {

    private static final Duration UPDATE_TOLERANCE = Duration.ofSeconds(2);

    private final InstallmentRepository repo;
    private final ActivityNarrator narrator;

    public InstallmentActivitySource(InstallmentRepository repo, ActivityNarrator narrator) {
        this.repo = repo;
        this.narrator = narrator;
    }

    public List<ActivityItemDTO> forProject(UUID projectId, UUID callerId) {
        List<Installment> rows = repo.findAllByProject(projectId);
        List<ActivityItemDTO> out = new ArrayList<>();
        for (Installment i : rows) {
            // CREATED — always
            out.add(buildCreated(i));

            boolean cancelled = i.getManualStatus() == InstallmentManualStatus.CANCELLED;
            boolean meaningfulUpdate = i.getUpdatedAt() != null
                    && i.getCreatedAt() != null
                    && Duration.between(i.getCreatedAt(), i.getUpdatedAt()).compareTo(UPDATE_TOLERANCE) > 0;

            if (cancelled) {
                out.add(buildCancelled(i));
            } else if (meaningfulUpdate) {
                out.add(buildUpdated(i));
            }
        }
        return out;
    }

    public void narrate(List<ActivityItemDTO> items, UUID callerId, Map<UUID, String> nameMap) {
        for (ActivityItemDTO item : items) {
            String actor = narrator.formatActor(item.getActorUserId(), callerId, nameMap);
            String verb = (String) item.getExtraData().get("__verb");
            String ctx = (String) item.getExtraData().get("__context");
            item.setTitle(actor + " " + verb);
            item.setSubtitle(ctx);
            item.getExtraData().remove("__verb");
            item.getExtraData().remove("__context");
        }
    }

    // -- builders -------------------------------------------------------------

    private ActivityItemDTO buildCreated(Installment i) {
        Map<String, Object> extras = baseExtras(i);
        extras.put("__verb", "added an installment of " + narrator.formatAmount(i.getExpectedAmount()));
        extras.put("__context", "Due " + i.getDueDate() + " · " + i.getTitle());
        return narrator.base(ActivityType.INSTALLMENT_CREATED, i.getId(), i.getCreatedAt(),
                        FinancialVisibilityScope.OFFICIAL)
                .actorUserId(i.getCreatedByUserId())
                .amount(i.getExpectedAmount())
                .linkedEntityType("INSTALLMENT")
                .linkedEntityId(i.getId())
                .extraData(extras)
                .build();
    }

    private ActivityItemDTO buildUpdated(Installment i) {
        Map<String, Object> extras = baseExtras(i);
        extras.put("__verb", "updated installment " + i.getTitle());
        extras.put("__context", "Now " + narrator.formatAmount(i.getExpectedAmount()) + " · due " + i.getDueDate());
        return narrator.base(ActivityType.INSTALLMENT_UPDATED, i.getId(), i.getUpdatedAt(),
                        FinancialVisibilityScope.OFFICIAL)
                .actorUserId(i.getCreatedByUserId())
                .amount(i.getExpectedAmount())
                .linkedEntityType("INSTALLMENT")
                .linkedEntityId(i.getId())
                .extraData(extras)
                .build();
    }

    private ActivityItemDTO buildCancelled(Installment i) {
        Map<String, Object> extras = baseExtras(i);
        extras.put("__verb", "cancelled installment " + i.getTitle());
        extras.put("__context", narrator.formatAmount(i.getExpectedAmount()) + " · due " + i.getDueDate());
        return narrator.base(ActivityType.INSTALLMENT_CANCELLED, i.getId(), i.getUpdatedAt(),
                        FinancialVisibilityScope.OFFICIAL)
                .actorUserId(i.getCreatedByUserId())
                .amount(i.getExpectedAmount())
                .linkedEntityType("INSTALLMENT")
                .linkedEntityId(i.getId())
                .badge("CANCELLED")
                .status(InstallmentManualStatus.CANCELLED.name())
                .extraData(extras)
                .build();
    }

    private Map<String, Object> baseExtras(Installment i) {
        Map<String, Object> m = new HashMap<>();
        m.put("installmentTitle", i.getTitle());
        m.put("dueDate", i.getDueDate().toString());
        m.put("manualStatus", i.getManualStatus().name());
        return m;
    }
}
