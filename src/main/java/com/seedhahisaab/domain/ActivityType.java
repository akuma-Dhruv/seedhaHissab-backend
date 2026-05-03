package com.seedhahisaab.domain;

/**
 * Type tag for {@code ActivityItemDTO}.
 *
 * <p>Activity items are <strong>derived</strong> from existing entities at
 * read time — no row is ever inserted into an activities table because no
 * such table exists. The set of types here is intentionally generic and
 * presentation-oriented so adding a new aggregator (e.g. a future vendor
 * ledger) is a one-line enum addition plus one mapper method.
 *
 * <p>The {@link #HIDDEN_PARTNER_RESTORED} value is reserved for forward
 * compatibility but is <em>never emitted in v1</em> — restore-history
 * cannot be reconstructed without a column we have explicitly chosen not
 * to add yet (see hidden-partners design notes). When a real activity
 * persistence layer arrives this enum value will start being used; until
 * then keeping it in the enum prevents an ABI break later.
 */
public enum ActivityType {
    TRANSACTION_CREATED,
    TRANSACTION_UPDATED,
    TRANSACTION_OMITTED,

    REMINDER_CREATED,
    REMINDER_COMPLETED,
    REMINDER_SNOOZED,
    REMINDER_ARCHIVED,

    INSTALLMENT_CREATED,
    INSTALLMENT_UPDATED,
    INSTALLMENT_CANCELLED,
    INSTALLMENT_PAYMENT_RECORDED,

    HIDDEN_PARTNER_CREATED,
    HIDDEN_PARTNER_UPDATED,
    HIDDEN_PARTNER_ARCHIVED,
    HIDDEN_PARTNER_RESTORED,

    SYSTEM_EVENT
}
