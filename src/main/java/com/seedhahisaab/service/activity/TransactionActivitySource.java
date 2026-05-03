package com.seedhahisaab.service.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.activity.ActivityItemDTO;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps {@link Transaction} rows into {@link ActivityItemDTO}s.
 *
 * <p>Per the version-aware emission strategy:
 * <ul>
 *   <li>{@code version == 1} → {@link ActivityType#TRANSACTION_CREATED}
 *       (or {@link ActivityType#INSTALLMENT_PAYMENT_RECORDED} when the row
 *       carries a {@code linkedInstallmentId}, since the create event for
 *       a payment is more meaningfully narrated as a payment).</li>
 *   <li>{@code version > 1} → {@link ActivityType#TRANSACTION_UPDATED}.</li>
 *   <li>{@code status == OMITTED} → {@link ActivityType#TRANSACTION_OMITTED}
 *       (in addition to whatever CREATED/UPDATED event the row also represents,
 *       narrated as a single row with the OMITTED type taking precedence).</li>
 * </ul>
 *
 * <p>Transactions are always {@link FinancialVisibilityScope#OFFICIAL} —
 * the hidden-partner overlay never participates in this code path.
 */
@Component
public class TransactionActivitySource {

    private final TransactionRepository repo;
    private final ActivityNarrator narrator;

    public TransactionActivitySource(TransactionRepository repo, ActivityNarrator narrator) {
        this.repo = repo;
        this.narrator = narrator;
    }

    public List<ActivityItemDTO> forProject(UUID projectId, UUID callerId, int fetchCap) {
        List<Transaction> rows = repo.findAllProjectVersionsForActivity(projectId, fetchCap);
        return rows.stream().map(t -> mapRow(t, callerId, /* personal */ false)).toList();
    }

    public List<ActivityItemDTO> forPersonal(UUID callerId, int fetchCap) {
        List<Transaction> rows = repo.findAllPersonalVersionsForActivity(callerId, fetchCap);
        return rows.stream().map(t -> mapRow(t, callerId, /* personal */ true)).toList();
    }

    public List<ActivityItemDTO> forCounterparty(UUID callerId, String name, int fetchCap) {
        List<Transaction> rows = repo.findAllCounterpartyVersionsForActivity(callerId, name, fetchCap);
        return rows.stream().map(t -> mapRow(t, callerId, /* personal */ true)).toList();
    }

    /** No actor batching needed here — caller does it via {@link ActivityNarrator}. */
    public void collectActorIds(List<ActivityItemDTO> items, java.util.Set<UUID> bucket) {
        for (ActivityItemDTO i : items) {
            if (i.getActorUserId() != null) bucket.add(i.getActorUserId());
        }
    }

    public void narrate(List<ActivityItemDTO> items, UUID callerId, Map<UUID, String> nameMap) {
        for (ActivityItemDTO i : items) {
            String actor = narrator.formatActor(i.getActorUserId(), callerId, nameMap);
            String verbAndAmount = (String) i.getExtraData().get("__verb");
            String contextLine = (String) i.getExtraData().get("__context");
            i.setTitle(actor + " " + verbAndAmount);
            i.setSubtitle(contextLine);
            // Keep extras minimal in the response — strip the narration scratch fields.
            i.getExtraData().remove("__verb");
            i.getExtraData().remove("__context");
        }
    }

    // -- internals -----------------------------------------------------------

    private ActivityItemDTO mapRow(Transaction t, UUID callerId, boolean personal) {
        ActivityType type;
        String verb;

        boolean isOmitted = t.getStatus() == TransactionStatus.OMITTED;
        boolean isUpdate = t.getVersion() > 1;
        boolean isPayment = t.getLinkedInstallmentId() != null
                && t.getType() == TransactionType.INCOME;

        if (isOmitted) {
            type = ActivityType.TRANSACTION_OMITTED;
            verb = "omitted " + humanPhrase(t.getType()) + " of " + narrator.formatAmount(t.getAmount());
        } else if (isUpdate) {
            type = ActivityType.TRANSACTION_UPDATED;
            verb = "updated " + humanPhrase(t.getType()) + " (now " + narrator.formatAmount(t.getAmount()) + ")";
        } else if (isPayment) {
            type = ActivityType.INSTALLMENT_PAYMENT_RECORDED;
            verb = "recorded a payment of " + narrator.formatAmount(t.getAmount());
        } else {
            type = ActivityType.TRANSACTION_CREATED;
            verb = "recorded " + humanPhrase(t.getType()) + " of " + narrator.formatAmount(t.getAmount());
        }

        Map<String, Object> extras = new HashMap<>();
        extras.put("transactionType", t.getType().name());
        extras.put("version", t.getVersion());
        extras.put("rootTransactionId", t.getRootTransactionId());
        if (t.getLinkedInstallmentId() != null) extras.put("linkedInstallmentId", t.getLinkedInstallmentId());
        if (t.getCounterpartyName() != null) extras.put("counterpartyName", t.getCounterpartyName());
        extras.put("__verb", verb);
        extras.put("__context", buildContext(t, personal));

        return narrator.base(type, t.getId(), t.getCreatedAt(), FinancialVisibilityScope.OFFICIAL)
                .actorUserId(t.getCreatedBy())
                .amount(t.getAmount())
                .linkedEntityType(isPayment ? "INSTALLMENT_PAYMENT" : "TRANSACTION")
                .linkedEntityId(isPayment ? t.getLinkedInstallmentId() : t.getId())
                .badge(isOmitted ? "OMITTED" : null)
                .status(t.getStatus().name())
                .extraData(extras)
                .build();
    }

    private String buildContext(Transaction t, boolean personal) {
        List<String> parts = new ArrayList<>();
        if (personal && t.getCounterpartyName() != null && !t.getCounterpartyName().isBlank()) {
            parts.add("Counterparty: " + t.getCounterpartyName());
        }
        if (t.getPurpose() != null && !t.getPurpose().isBlank()) {
            parts.add(t.getPurpose());
        }
        return String.join(" · ", parts);
    }

    private String humanPhrase(TransactionType type) {
        return switch (type) {
            case EXPENSE -> "an expense";
            case INCOME -> "income";
            case VENDOR_SUPPLY -> "a vendor supply";
            case VENDOR_PAYMENT -> "a vendor payment";
            case PARTNER_SETTLEMENT -> "a partner settlement";
            case PROFIT_WITHDRAWAL -> "a profit withdrawal";
            case LEND -> "a loan given";
            case BORROW -> "a loan taken";
            case REPAYMENT_GIVEN -> "a repayment paid";
            case REPAYMENT_RECEIVED -> "a repayment received";
        };
    }
}
