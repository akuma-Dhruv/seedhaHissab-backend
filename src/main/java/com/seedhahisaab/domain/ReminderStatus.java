package com.seedhahisaab.domain;

/**
 * Lifecycle states for a {@link com.seedhahisaab.entity.Reminder}.
 *
 * <p>Reminders are <strong>workflow metadata</strong>, not financial data —
 * none of these states represent payment, balance, or settlement; the
 * underlying transaction ledger remains the single source of financial truth.
 *
 * <ul>
 *   <li>{@link #PENDING} — newly created, awaiting action.</li>
 *   <li>{@link #COMPLETED} — user has marked the action as done.</li>
 *   <li>{@link #SNOOZED} — due date pushed forward; logically still pending.</li>
 *   <li>{@link #ARCHIVED} — soft-deleted; immutable, hidden from default lists.</li>
 * </ul>
 */
public enum ReminderStatus {
    PENDING,
    COMPLETED,
    SNOOZED,
    ARCHIVED
}
