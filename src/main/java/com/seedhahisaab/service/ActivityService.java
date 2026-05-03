package com.seedhahisaab.service;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.dto.activity.ActivityFeedResponse;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.service.activity.ActivityNarrator;
import com.seedhahisaab.service.activity.HiddenPartnerActivitySource;
import com.seedhahisaab.service.activity.InstallmentActivitySource;
import com.seedhahisaab.service.activity.ReminderActivitySource;
import com.seedhahisaab.service.activity.TransactionActivitySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates the three timeline endpoints (project / personal / counterparty).
 *
 * <p>The service is intentionally simple: pull from each source, narrate
 * server-side, merge by timestamp, slice the requested window. Nothing is
 * stored. Nothing recomputes financial truth — every figure rendered here
 * was pulled straight from the underlying entity row.
 *
 * <p><strong>Privacy is enforced at three layers</strong> (defence in depth):
 * <ol>
 *   <li><em>Repository layer</em> — every hidden-partner read goes through
 *       a creator-scoped query. Even if any other layer mistakenly let a
 *       non-creator pass, the data layer returns no hidden rows.</li>
 *   <li><em>Service layer</em> — project access is required up-front via
 *       {@link ProjectService#requireProject(UUID, UUID)}, which 404s for
 *       projects the caller doesn't own.</li>
 *   <li><em>Merge layer</em> — the optional {@code visibilityScope} filter
 *       is applied AFTER merging so it can never accidentally widen the
 *       set; it can only narrow it.</li>
 * </ol>
 */
@Service
public class ActivityService {

    private final ProjectService projectService;
    private final TransactionActivitySource txns;
    private final InstallmentActivitySource installments;
    private final ReminderActivitySource reminders;
    private final HiddenPartnerActivitySource hidden;
    private final ActivityNarrator narrator;

    public ActivityService(ProjectService projectService,
                           TransactionActivitySource txns,
                           InstallmentActivitySource installments,
                           ReminderActivitySource reminders,
                           HiddenPartnerActivitySource hidden,
                           ActivityNarrator narrator) {
        this.projectService = projectService;
        this.txns = txns;
        this.installments = installments;
        this.reminders = reminders;
        this.hidden = hidden;
        this.narrator = narrator;
    }

    @Transactional(readOnly = true)
    public ActivityFeedResponse forProject(UUID projectId,
                                           UUID callerId,
                                           int page,
                                           int limit,
                                           ActivityType typeFilter,
                                           FinancialVisibilityScope scopeFilter) {
        validatePaging(page, limit);
        // Layer 2: project access guard. 404 if caller doesn't own the project.
        projectService.requireProject(projectId, callerId);
        int offset = page * limit;
        int fetchCap = ActivityNarrator.perSourceFetchCount(offset, limit);

        List<ActivityItemDTO> all = new ArrayList<>();
        all.addAll(txns.forProject(projectId, callerId, fetchCap));
        all.addAll(installments.forProject(projectId, callerId));
        all.addAll(reminders.forProject(projectId, callerId, fetchCap));
        // Layer 1: hidden source itself is creator-scoped at the repository.
        all.addAll(hidden.forProject(projectId, callerId));

        return narrateMergeAndSlice(all, callerId, page, limit, offset, typeFilter, scopeFilter);
    }

    @Transactional(readOnly = true)
    public ActivityFeedResponse forPersonal(UUID callerId,
                                            int page,
                                            int limit,
                                            ActivityType typeFilter,
                                            FinancialVisibilityScope scopeFilter) {
        validatePaging(page, limit);
        int offset = page * limit;
        int fetchCap = ActivityNarrator.perSourceFetchCount(offset, limit);

        List<ActivityItemDTO> all = new ArrayList<>();
        all.addAll(txns.forPersonal(callerId, fetchCap));
        all.addAll(reminders.forUser(callerId, fetchCap));

        return narrateMergeAndSlice(all, callerId, page, limit, offset, typeFilter, scopeFilter);
    }

    @Transactional(readOnly = true)
    public ActivityFeedResponse forCounterparty(UUID callerId,
                                                String name,
                                                int page,
                                                int limit,
                                                ActivityType typeFilter,
                                                FinancialVisibilityScope scopeFilter) {
        validatePaging(page, limit);
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("Counterparty name is required.");
        }
        String trimmed = name.trim();
        int offset = page * limit;
        int fetchCap = ActivityNarrator.perSourceFetchCount(offset, limit);

        List<ActivityItemDTO> all = new ArrayList<>();
        all.addAll(txns.forCounterparty(callerId, trimmed, fetchCap));
        all.addAll(reminders.forCounterparty(callerId, trimmed, fetchCap));

        return narrateMergeAndSlice(all, callerId, page, limit, offset, typeFilter, scopeFilter);
    }

    // -- internals ------------------------------------------------------------

    private ActivityFeedResponse narrateMergeAndSlice(List<ActivityItemDTO> all,
                                                      UUID callerId,
                                                      int page,
                                                      int limit,
                                                      int offset,
                                                      ActivityType typeFilter,
                                                      FinancialVisibilityScope scopeFilter) {
        // Batched actor lookup ONCE per request — no per-row fetches.
        Set<UUID> actorIds = new HashSet<>();
        for (ActivityItemDTO i : all) {
            if (i.getActorUserId() != null) actorIds.add(i.getActorUserId());
        }
        Map<UUID, String> nameMap = narrator.buildActorNameMap(actorIds, callerId);

        // Each source narrates its own rows (so source-specific phrasing stays
        // colocated with source-specific row mapping). After this, every
        // ActivityItemDTO has title/subtitle filled in and scratch fields
        // stripped from extraData.
        narrateBySource(all, callerId, nameMap);

        // Filters (applied post-merge so they can only narrow, never widen).
        List<ActivityItemDTO> filtered = all.stream()
                .filter(i -> typeFilter == null || i.getType() == typeFilter)
                .filter(i -> scopeFilter == null || i.getVisibilityScope() == scopeFilter)
                .sorted(Comparator.comparing(ActivityItemDTO::getTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        boolean hasMore = filtered.size() > offset + limit;
        List<ActivityItemDTO> window = filtered.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        return ActivityFeedResponse.builder()
                .items(window)
                .page(page)
                .limit(limit)
                .hasMore(hasMore)
                .build();
    }

    private void narrateBySource(List<ActivityItemDTO> all, UUID callerId, Map<UUID, String> nameMap) {
        // Group by linkedEntityType so each source narrator runs once over
        // its own slice. Cheap; the list is small.
        List<ActivityItemDTO> txnRows = new ArrayList<>();
        List<ActivityItemDTO> instRows = new ArrayList<>();
        List<ActivityItemDTO> remRows = new ArrayList<>();
        List<ActivityItemDTO> hiddenRows = new ArrayList<>();
        for (ActivityItemDTO i : all) {
            String t = i.getLinkedEntityType();
            if ("TRANSACTION".equals(t) || "INSTALLMENT_PAYMENT".equals(t)) txnRows.add(i);
            else if ("INSTALLMENT".equals(t)) instRows.add(i);
            else if ("REMINDER".equals(t)) remRows.add(i);
            else if ("HIDDEN_PARTNER".equals(t)) hiddenRows.add(i);
        }
        txns.narrate(txnRows, callerId, nameMap);
        installments.narrate(instRows, callerId, nameMap);
        reminders.narrate(remRows, callerId, nameMap);
        hidden.narrate(hiddenRows, callerId, nameMap);
    }

    private void validatePaging(int page, int limit) {
        if (page < 0) throw ApiException.badRequest("page must be >= 0");
        if (limit < 1 || limit > 100) throw ApiException.badRequest("limit must be between 1 and 100");
    }
}
