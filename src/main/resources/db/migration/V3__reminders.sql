-- =============================================================================
-- V3 — Reminder & Due Tracking engine
-- =============================================================================
-- Lightweight follow-up notes attached to financial contexts. Pure metadata —
-- the schema deliberately has NO amount column and NO balance fields, because
-- reminders never carry financial truth (transactions remain the only source).
-- =============================================================================

CREATE TABLE IF NOT EXISTS reminders (
    id                         UUID PRIMARY KEY,
    title                      VARCHAR(255) NOT NULL,
    description                VARCHAR(1024),
    due_date                   DATE         NOT NULL,
    status                     VARCHAR(32)  NOT NULL,
    linked_transaction_id      UUID,
    linked_project_id          UUID,
    linked_counterparty_name   VARCHAR(255),
    created_by_user_id         UUID         NOT NULL,
    created_at                 TIMESTAMPTZ  NOT NULL,
    updated_at                 TIMESTAMPTZ  NOT NULL,
    CONSTRAINT reminders_status_check
        CHECK (status IN ('PENDING', 'COMPLETED', 'SNOOZED', 'ARCHIVED'))
);

-- Dashboard / bucket queries (today, overdue, upcoming) sort and filter by
-- (owner, due_date), so this is the hot index.
CREATE INDEX IF NOT EXISTS idx_rem_owner_due
    ON reminders (created_by_user_id, due_date);

-- Project widget filter.
CREATE INDEX IF NOT EXISTS idx_rem_owner_project
    ON reminders (created_by_user_id, linked_project_id);

-- Transaction-context filter (rare in v1 but cheap to maintain).
CREATE INDEX IF NOT EXISTS idx_rem_owner_root_txn
    ON reminders (created_by_user_id, linked_transaction_id);

-- Status filter (e.g. "show completed").
CREATE INDEX IF NOT EXISTS idx_rem_owner_status
    ON reminders (created_by_user_id, status);

-- Counterparty widget filter — case-insensitive equality.
CREATE INDEX IF NOT EXISTS idx_rem_owner_cp_name_lower
    ON reminders (created_by_user_id, lower(linked_counterparty_name));
