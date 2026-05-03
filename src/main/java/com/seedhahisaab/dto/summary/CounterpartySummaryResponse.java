package com.seedhahisaab.dto.summary;

import com.seedhahisaab.domain.PersonalLedgerSign;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Per-counterparty aggregated balance for a user, derived purely from
 * latest-active personal transactions.
 */
@Data
@AllArgsConstructor
public class CounterpartySummaryResponse {
    /** Display name (case as last typed by the user). */
    private String counterpartyName;
    /** Total amount the user gave to this counterparty (positive ledger types). */
    private BigDecimal totalGiven;
    /** Total amount the user received from this counterparty (negative ledger types). */
    private BigDecimal totalReceived;
    /** totalGiven - totalReceived. Positive => they owe me; negative => I owe them. */
    private BigDecimal netBalance;
    /** Human-friendly direction tag: THEY_OWE_ME / I_OWE_THEM / SETTLED. */
    private PersonalLedgerSign.Direction direction;
}
