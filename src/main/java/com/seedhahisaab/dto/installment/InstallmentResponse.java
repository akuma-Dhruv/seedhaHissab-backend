package com.seedhahisaab.dto.installment;

import com.seedhahisaab.domain.InstallmentDerivedStatus;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Wire shape returned by every installment endpoint.
 *
 * <p>{@code receivedAmount}, {@code remainingAmount}, and {@code status} are
 * <strong>derived server-side</strong> by {@code InstallmentService}. Frontend
 * MUST render these as-is and MUST NOT recompute. {@code overCollected} is a
 * convenience flag for the UI hint when received exceeds expected.
 *
 * <p>{@code linkedPayments} is populated only by the single-installment
 * endpoint; list endpoints leave it null/empty for payload size reasons.
 */
@Data
@Builder
public class InstallmentResponse {

    private UUID id;
    private UUID projectId;
    private UUID customerId;
    private String customerName;
    private String title;
    private String description;
    private BigDecimal expectedAmount;
    private LocalDate dueDate;

    /** Sum of latest-version, ACTIVE, INCOME transactions linked to this installment. Derived. */
    private BigDecimal receivedAmount;

    /** max(0, expectedAmount - receivedAmount). Derived. */
    private BigDecimal remainingAmount;

    /** True when receivedAmount > expectedAmount. Surfaces the "Over-collected by ₹X" UI hint. */
    private boolean overCollected;

    /** Backend-derived authoritative status. Frontend renders only — never recomputes. */
    private InstallmentDerivedStatus status;

    /** Optional, populated only by GET /installments/{id}. */
    private List<TransactionResponse> linkedPayments;

    private UUID createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
