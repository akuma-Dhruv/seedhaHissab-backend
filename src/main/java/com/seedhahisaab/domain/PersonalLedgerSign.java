package com.seedhahisaab.domain;

import java.util.Set;

/**
 * Centralized sign convention for personal counterparty balances.
 *
 * Positive (+): I am out money, the counterparty owes me.
 *   - LEND              : I gave money to them
 *   - REPAYMENT_GIVEN   : I repaid money to them (reduces what I owe them)
 *   - EXPENSE w/ cp     : I spent money on / for them
 *
 * Negative (-): I have received money, I owe the counterparty.
 *   - BORROW            : I received money from them
 *   - REPAYMENT_RECEIVED: They repaid me (reduces what they owe me)
 *   - INCOME w/ cp      : I received money from them
 *
 * EXPENSE / INCOME only participate in counterparty balances when a
 * counterparty name is present on the transaction. Without a counterparty,
 * they remain plain personal cash-flow entries that affect totalIncome /
 * totalExpense but never any ledger.
 *
 * The sign decision is intentionally kept here (and only here) so backend
 * responses are the single source of truth — frontend never re-derives it.
 */
public final class PersonalLedgerSign {

    private PersonalLedgerSign() {}

    /** Personal-transaction types allowed by the API. */
    public static final Set<TransactionType> ALLOWED_PERSONAL_TYPES = Set.of(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.LEND,
            TransactionType.BORROW,
            TransactionType.REPAYMENT_GIVEN,
            TransactionType.REPAYMENT_RECEIVED
    );

    /** Types that always require a counterparty name. */
    public static final Set<TransactionType> COUNTERPARTY_REQUIRED_TYPES = Set.of(
            TransactionType.LEND,
            TransactionType.BORROW,
            TransactionType.REPAYMENT_GIVEN,
            TransactionType.REPAYMENT_RECEIVED
    );

    /** Types that contribute +amount to counterparty balance (they owe me more). */
    public static final Set<TransactionType> POSITIVE_LEDGER_TYPES = Set.of(
            TransactionType.LEND,
            TransactionType.REPAYMENT_GIVEN,
            TransactionType.EXPENSE
    );

    /** Types that contribute -amount to counterparty balance (I owe them more). */
    public static final Set<TransactionType> NEGATIVE_LEDGER_TYPES = Set.of(
            TransactionType.BORROW,
            TransactionType.REPAYMENT_RECEIVED,
            TransactionType.INCOME
    );

    /** True if the type is one of the four explicit borrow/lend types. */
    public static boolean isExplicitLedgerType(TransactionType type) {
        return COUNTERPARTY_REQUIRED_TYPES.contains(type);
    }

    /** Direction string returned with each counterparty summary row. */
    public enum Direction { THEY_OWE_ME, I_OWE_THEM, SETTLED }

    public static Direction direction(java.math.BigDecimal netBalance) {
        int s = netBalance.signum();
        if (s > 0) return Direction.THEY_OWE_ME;
        if (s < 0) return Direction.I_OWE_THEM;
        return Direction.SETTLED;
    }
}
