package com.seedhahisaab.domain;

/**
 * The <em>persisted</em> portion of an installment's status.
 *
 * <p>Only two values are stored on the row: {@link #PENDING} (the default,
 * "this is an open expected receivable") and {@link #CANCELLED} (the user
 * explicitly marked the receivable as no longer expected).
 *
 * <p>All other user-facing statuses — {@code PARTIALLY_RECEIVED},
 * {@code RECEIVED}, {@code OVERDUE} — are <strong>derived at read time</strong>
 * by {@link com.seedhahisaab.service.InstallmentService} from the sum of
 * linked INCOME transactions and today's date in Asia/Kolkata. They are
 * never written to the database.
 */
public enum InstallmentManualStatus {
    PENDING,
    CANCELLED
}
