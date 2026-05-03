package com.seedhahisaab.service;

import com.seedhahisaab.domain.ReminderStatus;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.reminder.ReminderRequest;
import com.seedhahisaab.dto.reminder.ReminderResponse;
import com.seedhahisaab.dto.reminder.ReminderSnoozeRequest;
import com.seedhahisaab.entity.Reminder;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.ReminderRepository;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application logic for the Reminder & Due Tracking engine.
 *
 * <h2>Design rules enforced here</h2>
 * <ul>
 *   <li>Reminders are owned by their creator. Every read/write is
 *       scoped to the JWT user; even reminders attached to a shared project
 *       are private to whoever created them.</li>
 *   <li>Reminders carry no financial truth — there is no amount column,
 *       no balance, and no derivation from the underlying ledger.</li>
 *   <li>{@code linkedTransactionId} is normalised to the
 *       {@code root_transaction_id} so the link survives edits and omits.</li>
 *   <li>Linked-project reminders require the user to have access to the
 *       project (via {@link ProjectService#requireProject}).</li>
 *   <li>Archived reminders are immutable: any mutation returns 409.</li>
 *   <li>"Today" is the current date in {@code Asia/Kolkata} (decision B);
 *       this avoids the UTC-midnight rollover making IST users see
 *       reminders flip to overdue overnight.</li>
 * </ul>
 */
@Service
public class ReminderService {

    /**
     * Single timezone used to compute "today" for bucket queries.
     * Lifted to a per-user setting in a future iteration.
     */
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final ReminderRepository reminderRepository;
    private final TransactionRepository transactionRepository;
    private final ProjectService projectService;

    public ReminderService(ReminderRepository reminderRepository,
                           TransactionRepository transactionRepository,
                           ProjectService projectService) {
        this.reminderRepository = reminderRepository;
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @Transactional
    public ReminderResponse create(ReminderRequest req, UUID userId) {
        UUID rootTxnId = resolveRootTransactionId(req.getLinkedTransactionId(), userId);
        UUID projectId = validateLinkedProject(req.getLinkedProjectId(), userId);
        String counterparty = normaliseCounterparty(req.getLinkedCounterpartyName());

        Reminder r = Reminder.builder()
                .id(UUID.randomUUID())
                .title(req.getTitle().trim())
                .description(emptyToNull(req.getDescription()))
                .dueDate(req.getDueDate())
                .status(ReminderStatus.PENDING)
                .linkedTransactionId(rootTxnId)
                .linkedProjectId(projectId)
                .linkedCounterpartyName(counterparty)
                .createdByUserId(userId)
                .build();
        return ReminderResponse.from(reminderRepository.save(r));
    }

    @Transactional
    public ReminderResponse update(UUID reminderId, ReminderRequest req, UUID userId) {
        Reminder r = requireOwnedAndMutable(reminderId, userId);
        UUID rootTxnId = resolveRootTransactionId(req.getLinkedTransactionId(), userId);
        UUID projectId = validateLinkedProject(req.getLinkedProjectId(), userId);
        String counterparty = normaliseCounterparty(req.getLinkedCounterpartyName());

        r.setTitle(req.getTitle().trim());
        r.setDescription(emptyToNull(req.getDescription()));
        r.setDueDate(req.getDueDate());
        r.setLinkedTransactionId(rootTxnId);
        r.setLinkedProjectId(projectId);
        r.setLinkedCounterpartyName(counterparty);
        return ReminderResponse.from(reminderRepository.save(r));
    }

    @Transactional
    public ReminderResponse complete(UUID reminderId, UUID userId) {
        Reminder r = requireOwnedAndMutable(reminderId, userId);
        r.setStatus(ReminderStatus.COMPLETED);
        return ReminderResponse.from(reminderRepository.save(r));
    }

    @Transactional
    public ReminderResponse snooze(UUID reminderId, ReminderSnoozeRequest req, UUID userId) {
        Reminder r = requireOwnedAndMutable(reminderId, userId);
        LocalDate today = todayInAppZone();
        if (req.getNewDueDate().isBefore(today)) {
            throw ApiException.badRequest("Snooze date must be today or later");
        }
        r.setDueDate(req.getNewDueDate());
        r.setStatus(ReminderStatus.SNOOZED);
        return ReminderResponse.from(reminderRepository.save(r));
    }

    @Transactional
    public ReminderResponse archive(UUID reminderId, UUID userId) {
        Reminder r = reminderRepository.findByIdAndCreatedByUserId(reminderId, userId)
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
        if (r.getStatus() == ReminderStatus.ARCHIVED) {
            // idempotent
            return ReminderResponse.from(r);
        }
        r.setStatus(ReminderStatus.ARCHIVED);
        return ReminderResponse.from(reminderRepository.save(r));
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public ReminderResponse get(UUID reminderId, UUID userId) {
        Reminder r = reminderRepository.findByIdAndCreatedByUserId(reminderId, userId)
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
        return ReminderResponse.from(r);
    }

    /**
     * Generic paged listing. {@code statusFilter} is a single status when
     * provided, else PENDING+SNOOZED by default; pass
     * {@code includeArchived=true} to see archived rows.
     */
    public PagedResponse<ReminderResponse> list(
            UUID userId,
            ReminderStatus statusFilter,
            boolean includeArchived,
            LocalDate dueAfter,
            LocalDate dueBefore,
            UUID projectId,
            String counterpartyName,
            int page,
            int limit) {

        List<ReminderStatus> statuses = statusFilter != null
                ? List.of(statusFilter)
                : null;

        String cpLower = counterpartyName == null
                ? null
                : counterpartyName.trim().toLowerCase();
        if (cpLower != null && cpLower.isEmpty()) cpLower = null;

        // Defensive clamp — surface bad pagination as 400 instead of letting
        // PageRequest.of() throw a 500 on negative/zero inputs.
        if (page < 0) {
            throw ApiException.badRequest("page must be >= 0");
        }
        if (limit < 1 || limit > 100) {
            throw ApiException.badRequest("limit must be between 1 and 100");
        }

        Page<Reminder> p = reminderRepository.search(
                userId,
                statuses,
                /* excludeArchived */ !includeArchived,
                dueAfter,
                dueBefore,
                projectId,
                cpLower,
                PageRequest.of(page, limit));

        List<ReminderResponse> data = p.getContent().stream()
                .map(ReminderResponse::from).collect(Collectors.toList());
        return new PagedResponse<>(data, page, limit, p.getTotalElements());
    }

    public List<ReminderResponse> today(UUID userId) {
        return reminderRepository.findToday(userId, todayInAppZone())
                .stream().map(ReminderResponse::from).collect(Collectors.toList());
    }

    public List<ReminderResponse> overdue(UUID userId) {
        return reminderRepository.findOverdue(userId, todayInAppZone())
                .stream().map(ReminderResponse::from).collect(Collectors.toList());
    }

    public List<ReminderResponse> upcoming(UUID userId) {
        LocalDate today = todayInAppZone();
        return reminderRepository.findUpcoming(userId, today, today.plusDays(7))
                .stream().map(ReminderResponse::from).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Reminder requireOwnedAndMutable(UUID reminderId, UUID userId) {
        Reminder r = reminderRepository.findByIdAndCreatedByUserId(reminderId, userId)
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
        if (r.getStatus() == ReminderStatus.ARCHIVED) {
            throw ApiException.conflict("Archived reminders cannot be modified");
        }
        return r;
    }

    /**
     * Validates the user can read the linked transaction and returns its
     * stable {@code root_transaction_id}. Returns {@code null} when no
     * link is requested.
     */
    private UUID resolveRootTransactionId(UUID inputTxnId, UUID userId) {
        if (inputTxnId == null) return null;
        Transaction txn = transactionRepository.findById(inputTxnId)
                .orElseThrow(() -> ApiException.notFound("Linked transaction not found"));
        if (txn.getProjectId() != null) {
            projectService.requireProject(txn.getProjectId(), userId);
        } else if (!userId.equals(txn.getCreatedBy())
                && !userId.equals(txn.getOwnerUserId())) {
            throw ApiException.forbidden("Cannot link to that transaction");
        }
        return txn.getRootTransactionId();
    }

    private UUID validateLinkedProject(UUID projectId, UUID userId) {
        if (projectId == null) return null;
        projectService.requireProject(projectId, userId);
        return projectId;
    }

    private static String normaliseCounterparty(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String emptyToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDate todayInAppZone() {
        return LocalDate.now(APP_ZONE);
    }
}
