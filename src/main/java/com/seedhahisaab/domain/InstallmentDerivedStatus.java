package com.seedhahisaab.domain;

/**
 * Full set of statuses surfaced to API clients for an installment.
 *
 * <p>Only the manual subset ({@code PENDING}, {@code CANCELLED}) is stored;
 * the rest are derived from received amount + due date. See
 * {@link com.seedhahisaab.service.InstallmentService} for the canonical
 * derivation rules — frontend MUST render this value as-is and MUST NOT
 * recompute it locally.
 */
public enum InstallmentDerivedStatus {
    /** No money received yet; due date in the future or today. */
    PENDING,
    /** Some money received but less than expected; not yet overdue. */
    PARTIALLY_RECEIVED,
    /** Received amount &gt;= expected amount. Terminal (until cancelled). */
    RECEIVED,
    /** Due date is in the past and remaining amount &gt; 0. */
    OVERDUE,
    /** Manually cancelled by the user; residual income (if any) remains valid. */
    CANCELLED
}
