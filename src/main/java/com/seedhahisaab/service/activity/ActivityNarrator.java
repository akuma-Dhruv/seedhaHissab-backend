package com.seedhahisaab.service.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.entity.User;
import com.seedhahisaab.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds human-readable, paste-friendly narration strings server-side so
 * web, mobile, exports, and any future notification surface render the
 * same wording.
 *
 * <p>Actor names are resolved with a single batched user lookup per request
 * (see {@link #buildActorNameMap}). The caller's row is rewritten to "You"
 * before narration so first-person feels natural.
 */
@Component
public class ActivityNarrator {

    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    private final UserRepository userRepository;

    public ActivityNarrator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * One-shot batched lookup. Pass every actor UUID present in the merged
     * source rows; the returned map contains a display name for each user
     * the caller is allowed to see by name (currently: any user — actor
     * names are not sensitive).
     */
    public Map<UUID, String> buildActorNameMap(Set<UUID> actorIds, UUID callerId) {
        if (actorIds == null || actorIds.isEmpty()) return Map.of();
        Set<UUID> filtered = new HashSet<>(actorIds);
        filtered.remove(null);
        if (filtered.isEmpty()) return Map.of();
        Map<UUID, String> map = new HashMap<>();
        for (User u : userRepository.findAllById(filtered)) {
            String name = u.getName();
            if (name == null || name.isBlank()) {
                String email = u.getEmail();
                name = (email == null) ? "User" : email.split("@", 2)[0];
            }
            map.put(u.getId(), name);
        }
        return map;
    }

    public String formatActor(UUID actorId, UUID callerId, Map<UUID, String> nameMap) {
        if (actorId == null) return "Someone";
        if (actorId.equals(callerId)) return "You";
        return nameMap.getOrDefault(actorId, "A teammate");
    }

    public String formatAmount(BigDecimal amount) {
        if (amount == null) return "";
        return "₹" + INR.format(amount);
    }

    public String activityKey(ActivityType type, UUID entityId, Instant ts) {
        long ms = ts == null ? 0L : ts.toEpochMilli();
        return type.name() + ":" + (entityId == null ? "_" : entityId) + ":" + ms;
    }

    /**
     * Common builder shortcut used by every source. Keeps the construction
     * of {@link ActivityItemDTO} consistent (especially the activityKey
     * format and visibilityScope defaulting).
     */
    public ActivityItemDTO.ActivityItemDTOBuilder base(ActivityType type,
                                                       UUID entityId,
                                                       Instant ts,
                                                       FinancialVisibilityScope scope) {
        return ActivityItemDTO.builder()
                .activityKey(activityKey(type, entityId, ts))
                .type(type)
                .timestamp(ts)
                .visibilityScope(scope == null ? FinancialVisibilityScope.OFFICIAL : scope);
    }

    public Map<String, Object> mergeExtras(Map<String, Object>... extras) {
        Map<String, Object> out = new HashMap<>();
        if (extras == null) return out;
        for (Map<String, Object> e : extras) {
            if (e != null) out.putAll(e);
        }
        return out;
    }

    /**
     * Bound on per-source row pulls. Tuned for the v1 small-business scale —
     * a single project rarely exceeds a few hundred lifecycle events. If
     * deep pagination becomes common we'll switch to a keyset cursor.
     */
    public static final int PER_SOURCE_HARD_CAP = 500;

    /** How many rows each source should fetch given an offset+limit window. */
    public static int perSourceFetchCount(int offset, int limit) {
        long want = (long) offset + (long) limit + 1L;
        if (want > PER_SOURCE_HARD_CAP) return PER_SOURCE_HARD_CAP;
        return (int) want;
    }

    /** No-op helper to keep call-sites readable when there are no extras. */
    public List<ActivityItemDTO> noop() { return List.of(); }
}
