package com.seedhahisaab.service;

import com.seedhahisaab.domain.InstallmentDerivedStatus;
import com.seedhahisaab.domain.InstallmentManualStatus;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.installment.InstallmentRequest;
import com.seedhahisaab.dto.installment.InstallmentResponse;
import com.seedhahisaab.dto.installment.InstallmentSummaryResponse;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.entity.Customer;
import com.seedhahisaab.entity.Installment;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.InstallmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application logic for the Project Installment & Receivable Engine.
 *
 * <h2>Single source of truth for derived status</h2>
 * The frontend NEVER computes installment status, received total, or
 * remaining amount. Those values are produced exclusively by
 * {@link #toResponse(Installment, BigDecimal, LocalDate, java.util.List)} so
 * the rules can never drift between client and server.
 *
 * <h2>Design rules enforced here</h2>
 * <ul>
 *   <li>Receivable numbers are <strong>derived on read</strong> by summing
 *       latest-version, ACTIVE, INCOME transactions linked to each
 *       installment. Nothing is stored on {@link Installment} except the
 *       promise (expected amount + due date) and a binary manual status.</li>
 *   <li>"Today" uses {@code Asia/Kolkata} so client and server agree on
 *       OVERDUE bucketing — same convention reminders use.</li>
 *   <li>Cancelling never reverses a payment. Already-collected money
 *       remains as INCOME transactions and continues to count in
 *       {@code totalReceived}; only the cancelled installment's
 *       <em>expected</em> amount drops out of the planning roll-up.</li>
 *   <li>Project access is enforced via
 *       {@link ProjectService#requireProject}; everything is JWT-scoped.</li>
 * </ul>
 */
@Service
public class InstallmentService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final InstallmentRepository installmentRepository;
    private final ProjectService projectService;
    private final CustomerService customerService;

    public InstallmentService(InstallmentRepository installmentRepository,
                              ProjectService projectService,
                              CustomerService customerService) {
        this.installmentRepository = installmentRepository;
        this.projectService = projectService;
        this.customerService = customerService;
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @Transactional
    public InstallmentResponse create(UUID projectId, InstallmentRequest req, UUID userId) {
        projectService.requireProject(projectId, userId);
        Customer customer = customerService.requireOwned(req.getCustomerId(), userId);
        // Auto-attach the customer to the project so the project surfaces it.
        customerService.attachToProject(projectId, customer.getId(), userId);

        Installment i = Installment.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .customerId(customer.getId())
                .title(req.getTitle().trim())
                .description(emptyToNull(req.getDescription()))
                .expectedAmount(req.getExpectedAmount())
                .dueDate(req.getDueDate())
                .manualStatus(InstallmentManualStatus.PENDING)
                .createdByUserId(userId)
                .build();
        Installment saved = installmentRepository.save(i);
        return toResponse(saved, customer.getName(), todayInAppZone(), null);
    }

    @Transactional
    public InstallmentResponse update(UUID installmentId, InstallmentRequest req, UUID userId) {
        Installment i = requireOwnedAndMutable(installmentId, userId);
        Customer customer = customerService.requireOwned(req.getCustomerId(), userId);
        if (!customer.getId().equals(i.getCustomerId())) {
            customerService.attachToProject(i.getProjectId(), customer.getId(), userId);
        }
        i.setCustomerId(customer.getId());
        i.setTitle(req.getTitle().trim());
        i.setDescription(emptyToNull(req.getDescription()));
        i.setExpectedAmount(req.getExpectedAmount());
        i.setDueDate(req.getDueDate());
        Installment saved = installmentRepository.save(i);
        return toResponse(saved, customer.getName(), todayInAppZone(), null);
    }

    @Transactional
    public InstallmentResponse cancel(UUID installmentId, UUID userId) {
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Installment not found"));
        i.setManualStatus(InstallmentManualStatus.CANCELLED);
        Installment saved = installmentRepository.save(i);
        return toResponse(saved, lookupCustomerName(saved.getCustomerId(), userId),
                todayInAppZone(), null);
    }

    /**
     * Re-opens a previously cancelled installment. Manual-only flip; status
     * after re-open is derived as usual from received amount + due date.
     */
    @Transactional
    public InstallmentResponse reopen(UUID installmentId, UUID userId) {
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Installment not found"));
        i.setManualStatus(InstallmentManualStatus.PENDING);
        Installment saved = installmentRepository.save(i);
        return toResponse(saved, lookupCustomerName(saved.getCustomerId(), userId),
                todayInAppZone(), null);
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    /**
     * Returns a single installment with derived figures and the linked
     * payment list inlined (newest-first).
     */
    public InstallmentResponse get(UUID installmentId, UUID userId) {
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Installment not found"));
        // Auth: also ensure the project is reachable by this user.
        projectService.requireProject(i.getProjectId(), userId);
        List<Transaction> payments = installmentRepository
                .findLinkedPaymentsForInstallment(installmentId);
        return toResponse(i, lookupCustomerName(i.getCustomerId(), userId),
                todayInAppZone(), payments);
    }

    /**
     * Paged project-scoped listing. {@code overdueOnly} is a UI shortcut that
     * filters the page to OVERDUE entries; it is applied <em>after</em>
     * derivation because OVERDUE is a derived state, not a stored one.
     */
    public PagedResponse<InstallmentResponse> listByProject(
            UUID projectId,
            UUID userId,
            InstallmentDerivedStatus statusFilter,
            boolean overdueOnly,
            UUID customerId,
            int page,
            int limit) {

        projectService.requireProject(projectId, userId);

        if (page < 0) {
            throw ApiException.badRequest("page must be >= 0");
        }
        if (limit < 1 || limit > 100) {
            throw ApiException.badRequest("limit must be between 1 and 100");
        }

        // Map a status filter onto the manual filter where possible. Stored
        // states are {PENDING, CANCELLED}; everything else needs derivation.
        InstallmentManualStatus manualFilter = null;
        if (statusFilter == InstallmentDerivedStatus.CANCELLED) {
            manualFilter = InstallmentManualStatus.CANCELLED;
        } else if (statusFilter != null) {
            manualFilter = InstallmentManualStatus.PENDING;
        }

        boolean needsDerivedFilter = overdueOnly
                || (statusFilter != null
                    && statusFilter != InstallmentDerivedStatus.CANCELLED);

        LocalDate today = todayInAppZone();

        if (!needsDerivedFilter) {
            // Fast path: no derived filter, the DB page is the final page.
            Page<Installment> p = installmentRepository.searchByProject(
                    projectId, manualFilter, customerId, PageRequest.of(page, limit));
            Map<UUID, String> customerNames = bulkCustomerNames(p.getContent(), userId);
            Map<UUID, BigDecimal> received = bulkReceived(p.getContent());
            List<InstallmentResponse> data = p.getContent().stream()
                    .map(i -> toResponse(i,
                            customerNames.get(i.getCustomerId()),
                            received.getOrDefault(i.getId(), BigDecimal.ZERO),
                            today,
                            null))
                    .collect(Collectors.toList());
            return new PagedResponse<>(data, page, limit, p.getTotalElements());
        }

        // Derived-status path: derive over the full project-scoped result,
        // filter, then paginate. We cannot push the filter into SQL without
        // duplicating the derivation logic across two layers.
        final InstallmentManualStatus manualFilterFinal = manualFilter;
        final UUID customerIdFinal = customerId;
        List<Installment> all = installmentRepository.findAllByProject(projectId).stream()
                .filter(i -> manualFilterFinal == null || i.getManualStatus() == manualFilterFinal)
                .filter(i -> customerIdFinal == null || customerIdFinal.equals(i.getCustomerId()))
                .collect(Collectors.toList());

        Map<UUID, String> customerNames = bulkCustomerNames(all, userId);
        Map<UUID, BigDecimal> received = bulkReceived(all);

        List<InstallmentResponse> derived = all.stream()
                .map(i -> toResponse(i,
                        customerNames.get(i.getCustomerId()),
                        received.getOrDefault(i.getId(), BigDecimal.ZERO),
                        today,
                        null))
                .filter(r -> {
                    if (statusFilter != null && r.getStatus() != statusFilter) return false;
                    if (overdueOnly && r.getStatus() != InstallmentDerivedStatus.OVERDUE) return false;
                    return true;
                })
                .collect(Collectors.toList());

        long total = derived.size();
        int from = Math.min(page * limit, derived.size());
        int to = Math.min(from + limit, derived.size());
        return new PagedResponse<>(derived.subList(from, to), page, limit, total);
    }

    /**
     * Project-wide receivable roll-up. Cancelled installments are excluded
     * from {@code totalExpected} but their already-collected amounts still
     * count in {@code totalReceived} (because that money really arrived).
     */
    public InstallmentSummaryResponse summaryForProject(UUID projectId, UUID userId) {
        projectService.requireProject(projectId, userId);
        List<Installment> all = installmentRepository.findAllByProject(projectId);
        LocalDate today = todayInAppZone();
        Map<UUID, BigDecimal> received = bulkReceived(all);

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        Map<InstallmentDerivedStatus, Long> counts = new EnumMap<>(InstallmentDerivedStatus.class);
        for (InstallmentDerivedStatus s : InstallmentDerivedStatus.values()) counts.put(s, 0L);

        for (Installment i : all) {
            BigDecimal r = received.getOrDefault(i.getId(), BigDecimal.ZERO);
            BigDecimal remaining = remainingFor(i, r);
            InstallmentDerivedStatus status = deriveStatus(i, r, today);
            counts.merge(status, 1L, Long::sum);

            // Always count actually-received money in totalReceived.
            totalReceived = totalReceived.add(r);

            if (status != InstallmentDerivedStatus.CANCELLED) {
                totalExpected = totalExpected.add(i.getExpectedAmount());
            }
            if (status == InstallmentDerivedStatus.PENDING
                    || status == InstallmentDerivedStatus.PARTIALLY_RECEIVED
                    || status == InstallmentDerivedStatus.OVERDUE) {
                totalPending = totalPending.add(remaining);
            }
            if (status == InstallmentDerivedStatus.OVERDUE) {
                totalOverdue = totalOverdue.add(remaining);
            }
        }

        return InstallmentSummaryResponse.builder()
                .totalExpected(totalExpected)
                .totalReceived(totalReceived)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .installmentCounts(counts)
                .build();
    }

    // ------------------------------------------------------------------
    // Internal — single source of truth for derivation
    // ------------------------------------------------------------------

    /**
     * Maps a stored installment + its received-sum into the wire DTO.
     * Centralises every derived field (received, remaining, status, over-collected).
     */
    private InstallmentResponse toResponse(Installment i,
                                           String customerName,
                                           BigDecimal received,
                                           LocalDate today,
                                           List<Transaction> payments) {
        BigDecimal remaining = remainingFor(i, received);
        boolean overCollected = received.compareTo(i.getExpectedAmount()) > 0;
        InstallmentDerivedStatus status = deriveStatus(i, received, today);

        List<TransactionResponse> paymentDtos = payments == null ? null
                : payments.stream().map(TransactionResponse::from).collect(Collectors.toList());

        return InstallmentResponse.builder()
                .id(i.getId())
                .projectId(i.getProjectId())
                .customerId(i.getCustomerId())
                .customerName(customerName)
                .title(i.getTitle())
                .description(i.getDescription())
                .expectedAmount(i.getExpectedAmount())
                .dueDate(i.getDueDate())
                .receivedAmount(received)
                .remainingAmount(remaining)
                .overCollected(overCollected)
                .status(status)
                .linkedPayments(paymentDtos)
                .createdByUserId(i.getCreatedByUserId())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    /** Convenience overload that fetches received-sum for a single installment. */
    private InstallmentResponse toResponse(Installment i,
                                           String customerName,
                                           LocalDate today,
                                           List<Transaction> payments) {
        BigDecimal received = installmentRepository.sumReceivedForInstallment(i.getId());
        if (received == null) received = BigDecimal.ZERO;
        return toResponse(i, customerName, received, today, payments);
    }

    /**
     * The ONE place where derived status is computed. Any change to the rules
     * goes here and propagates everywhere.
     */
    private InstallmentDerivedStatus deriveStatus(Installment i,
                                                   BigDecimal received,
                                                   LocalDate today) {
        if (i.getManualStatus() == InstallmentManualStatus.CANCELLED) {
            return InstallmentDerivedStatus.CANCELLED;
        }
        BigDecimal remaining = remainingFor(i, received);
        if (remaining.signum() <= 0) {
            return InstallmentDerivedStatus.RECEIVED;
        }
        if (i.getDueDate().isBefore(today)) {
            return InstallmentDerivedStatus.OVERDUE;
        }
        if (received.signum() > 0) {
            return InstallmentDerivedStatus.PARTIALLY_RECEIVED;
        }
        return InstallmentDerivedStatus.PENDING;
    }

    private BigDecimal remainingFor(Installment i, BigDecimal received) {
        BigDecimal r = i.getExpectedAmount().subtract(received);
        return r.signum() < 0 ? BigDecimal.ZERO : r;
    }

    private Installment requireOwnedAndMutable(UUID installmentId, UUID userId) {
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Installment not found"));
        if (i.getManualStatus() == InstallmentManualStatus.CANCELLED) {
            throw ApiException.conflict(
                    "Cancelled installments cannot be edited. Re-open it first.");
        }
        return i;
    }

    private String lookupCustomerName(UUID customerId, UUID userId) {
        try {
            return customerService.get(customerId, userId).getName();
        } catch (ApiException e) {
            return null;
        }
    }

    private Map<UUID, String> bulkCustomerNames(List<Installment> installments, UUID userId) {
        Map<UUID, String> out = new HashMap<>();
        List<UUID> ids = installments.stream().map(Installment::getCustomerId).distinct().toList();
        // N customers per page; tiny, single-shot lookups are fine.
        for (UUID id : ids) {
            out.put(id, lookupCustomerName(id, userId));
        }
        return out;
    }

    private Map<UUID, BigDecimal> bulkReceived(List<Installment> installments) {
        if (installments.isEmpty()) return Map.of();
        List<UUID> ids = installments.stream().map(Installment::getId).toList();
        List<Object[]> rows = installmentRepository.sumReceivedForInstallments(ids);
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            BigDecimal sum = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            out.put(id, sum);
        }
        return out;
    }

    /**
     * Public hook for the transaction service to validate a linked-installment
     * id at write-time. Returns the installment's projectId so the caller can
     * ensure same-project consistency. Returns null when the input is null.
     */
    public Installment requireValidLinkTarget(UUID installmentId, UUID projectId, UUID userId) {
        if (installmentId == null) return null;
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Linked installment not found"));
        if (!i.getProjectId().equals(projectId)) {
            throw ApiException.badRequest(
                    "Income transaction and installment must belong to the same project");
        }
        if (i.getManualStatus() == InstallmentManualStatus.CANCELLED) {
            throw ApiException.badRequest(
                    "Cannot link payments to a cancelled installment");
        }
        return i;
    }

    /** Same as above but only validates ownership/access — used by reminders. */
    public Installment requireReadable(UUID installmentId, UUID userId) {
        if (installmentId == null) return null;
        Installment i = installmentRepository.findByIdAndCreatedByUserId(installmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Linked installment not found"));
        projectService.requireProject(i.getProjectId(), userId);
        return i;
    }

    private static String emptyToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDate todayInAppZone() {
        return LocalDate.now(APP_ZONE);
    }

    /**
     * For accumulating items into a buffer list inside a stream collector.
     * Kept private because it's only useful here.
     */
    @SuppressWarnings("unused")
    private static <T> ArrayList<T> emptyList() {
        return new ArrayList<>();
    }
}
