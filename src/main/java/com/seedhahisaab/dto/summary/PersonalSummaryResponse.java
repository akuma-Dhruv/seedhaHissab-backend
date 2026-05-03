package com.seedhahisaab.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregate snapshot of a user's personal cash-flow.
 *
 * - totalIncome / totalExpense: pure cash-flow (sum of all latest ACTIVE
 *   personal INCOME / EXPENSE rows, regardless of counterparty).
 *
 * - totalLent / totalBorrowed: explicit borrow/lend types only
 *   (sum of LEND / BORROW rows). Useful for "money out the door I expect back"
 *   and "money in the door I owe back".
 *
 * - totalReceivable / totalPayable: derived per-counterparty net balances:
 *     totalReceivable = sum of positive balances over all counterparties
 *     totalPayable    = sum of |negative balances| over all counterparties
 *   These already account for repayments and counterparty-tagged
 *   EXPENSE / INCOME entries.
 *
 * - netBalance = totalReceivable - totalPayable.
 */
@Data
@AllArgsConstructor
public class PersonalSummaryResponse {
    private UUID ownerUserId;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalLent;
    private BigDecimal totalBorrowed;
    private BigDecimal totalReceivable;
    private BigDecimal totalPayable;
    private BigDecimal netBalance;
}
