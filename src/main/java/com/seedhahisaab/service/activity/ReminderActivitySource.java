package com.seedhahisaab.service.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.domain.ReminderStatus;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.entity.Reminder;
import com.seedhahisaab.repository.ReminderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reminders are workflow metadata — they hold no money. The timeline still
 * tracks their lifecycle because users want to see when they nudged
 * themselves about something.
 *
 * <p>Status mapping:
 * <ul>
 *   <li>{@code COMPLETED}  → CREATED + COMPLETED rows (only if the gap
 *       between createdAt and updatedAt is meaningful).</li>
 *   <li>{@code SNOOZED}    → CREATED + SNOOZED rows.</li>
 *   <li>{@code ARCHIVED}   → CREATED + ARCHIVED rows.</li>
 *   <li>{@code PENDING}    → CREATED only.</li>
 * </ul>
 */
@Component
public class ReminderActivitySource {

    private static final Duration UPDATE_TOLERANCE = Duration.ofSeconds(2);

    private final ReminderRepository repo;
    private final ActivityNarrator narrator;

    public ReminderActivitySource(ReminderRepository repo, ActivityNarrator narrator) {
        this.repo = repo;
        this.narrator = narrator;
    }

    public List<ActivityItemDTO> forProject(UUID projectId, UUID callerId, int fetchCap) {
        return mapAll(repo.findProjectRemindersForActivity(callerId, projectId,
                PageRequest.of(0, fetchCap)));
    }

    public List<ActivityItemDTO> forUser(UUID callerId, int fetchCap) {
        return mapAll(repo.findAllForUserActivity(callerId, PageRequest.of(0, fetchCap)));
    }

    public List<ActivityItemDTO> forCounterparty(UUID callerId, String name, int fetchCap) {
        return mapAll(repo.findCounterpartyRemindersForActivity(callerId, name,
                PageRequest.of(0, fetchCap)));
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

    private List<ActivityItemDTO> mapAll(List<Reminder> rows) {
        List<ActivityItemDTO> out = new ArrayList<>();
        for (Reminder r : rows) {
            out.add(buildCreated(r));
            boolean meaningful = r.getCreatedAt() != null && r.getUpdatedAt() != null
                    && Duration.between(r.getCreatedAt(), r.getUpdatedAt()).compareTo(UPDATE_TOLERANCE) > 0;
            if (!meaningful) continue;
            switch (r.getStatus()) {
                case COMPLETED -> out.add(buildLifecycle(r, ActivityType.REMINDER_COMPLETED, "completed"));
                case SNOOZED -> out.add(buildLifecycle(r, ActivityType.REMINDER_SNOOZED, "snoozed"));
                case ARCHIVED -> out.add(buildLifecycle(r, ActivityType.REMINDER_ARCHIVED, "archived"));
                case PENDING -> { /* no-op */ }
            }
        }
        return out;
    }

    private ActivityItemDTO buildCreated(Reminder r) {
        Map<String, Object> extras = baseExtras(r);
        extras.put("__verb", "set a reminder: " + r.getTitle());
        extras.put("__context", "Due " + r.getDueDate());
        return narrator.base(ActivityType.REMINDER_CREATED, r.getId(), r.getCreatedAt(),
                        FinancialVisibilityScope.OFFICIAL)
                .actorUserId(r.getCreatedByUserId())
                .linkedEntityType("REMINDER")
                .linkedEntityId(r.getId())
                .status(ReminderStatus.PENDING.name())
                .extraData(extras)
                .build();
    }

    private ActivityItemDTO buildLifecycle(Reminder r, ActivityType type, String verbVerb) {
        Map<String, Object> extras = baseExtras(r);
        extras.put("__verb", verbVerb + " reminder: " + r.getTitle());
        extras.put("__context", r.getStatus() == ReminderStatus.SNOOZED && r.getDueDate() != null
                ? "Now due " + r.getDueDate()
                : "");
        return narrator.base(type, r.getId(), r.getUpdatedAt(), FinancialVisibilityScope.OFFICIAL)
                .actorUserId(r.getCreatedByUserId())
                .linkedEntityType("REMINDER")
                .linkedEntityId(r.getId())
                .badge(r.getStatus().name())
                .status(r.getStatus().name())
                .extraData(extras)
                .build();
    }

    private Map<String, Object> baseExtras(Reminder r) {
        Map<String, Object> m = new HashMap<>();
        m.put("reminderTitle", r.getTitle());
        if (r.getLinkedProjectId() != null) m.put("linkedProjectId", r.getLinkedProjectId());
        if (r.getLinkedTransactionId() != null) m.put("linkedTransactionId", r.getLinkedTransactionId());
        if (r.getLinkedInstallmentId() != null) m.put("linkedInstallmentId", r.getLinkedInstallmentId());
        if (r.getLinkedCounterpartyName() != null) m.put("linkedCounterpartyName", r.getLinkedCounterpartyName());
        return m;
    }
}
