package com.seedhahisaab.dto.installment;

import com.seedhahisaab.domain.InstallmentDerivedStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Roll-up of all (non-cancelled) installments in a project, derived from the
 * underlying transactions. Returned by
 * {@code GET /projects/{id}/installments/summary}.
 *
 * <p>Cancelled installments are excluded from {@code totalExpected},
 * {@code totalPending}, and {@code totalOverdue}; their already-collected
 * amounts still count in {@code totalReceived} because that money really did
 * arrive (and remains as INCOME transactions in the ledger).
 */
@Data
@Builder
public class InstallmentSummaryResponse {

    /** Σ expectedAmount over non-cancelled installments. */
    private BigDecimal totalExpected;

    /** Σ receivedAmount over ALL installments (including cancelled — the money still came in). */
    private BigDecimal totalReceived;

    /** Σ remainingAmount over PENDING + PARTIALLY_RECEIVED + OVERDUE. */
    private BigDecimal totalPending;

    /** Σ remainingAmount over OVERDUE only. */
    private BigDecimal totalOverdue;

    /** Counts keyed by derived status (zero-filled for any missing keys). */
    private Map<InstallmentDerivedStatus, Long> installmentCounts;
}
