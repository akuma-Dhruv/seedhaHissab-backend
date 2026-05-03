-- =============================================================================
-- V4 — Project Installment & Receivable Engine
-- =============================================================================
-- Adds three new tables and two nullable FK columns. The engine is a
-- *receivable-planning layer*; it never stores money figures that are already
-- represented by transactions:
--
--   - installments.expected_amount  — planning metadata (the *promise*)
--   - transactions (existing)       — financial truth (actual money received)
--
-- Derived figures (received_amount, remaining_amount, status) are NEVER stored.
-- They are computed at read-time in InstallmentService by summing
-- INCOME transactions whose linked_installment_id matches.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Customers — global per user (NOT scoped to a project). The same customer
-- may be linked into multiple projects via project_customers.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    phone                  VARCHAR(64),
    notes                  VARCHAR(1024),
    created_by_user_id     UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cust_owner_name_lower
    ON customers (created_by_user_id, lower(name));

-- -----------------------------------------------------------------------------
-- Project ↔ Customer link table. No balances stored here; this only records
-- "this customer is associated with this project". Same customer can sit in
-- multiple projects, which is the whole point of customers being global.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_customers (
    id           UUID PRIMARY KEY,
    project_id   UUID        NOT NULL,
    customer_id  UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_project_customer UNIQUE (project_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_pc_project ON project_customers (project_id);
CREATE INDEX IF NOT EXISTS idx_pc_customer ON project_customers (customer_id);

-- -----------------------------------------------------------------------------
-- Installments — expected receivables. Status column stores ONLY the manual
-- lifecycle state (PENDING or CANCELLED). All other states
-- (PARTIALLY_RECEIVED, RECEIVED, OVERDUE) are *derived* at read-time from the
-- linked income transactions and the due date. This keeps the table immune to
-- staleness and removes the need for any reconciliation cron.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS installments (
    id                     UUID PRIMARY KEY,
    project_id             UUID            NOT NULL,
    customer_id            UUID            NOT NULL,
    title                  VARCHAR(255)    NOT NULL,
    description            VARCHAR(1024),
    expected_amount        NUMERIC(19,4)   NOT NULL,
    due_date               DATE            NOT NULL,
    manual_status          VARCHAR(32)     NOT NULL,
    created_by_user_id     UUID            NOT NULL,
    created_at             TIMESTAMPTZ     NOT NULL,
    updated_at             TIMESTAMPTZ     NOT NULL,
    CONSTRAINT installments_manual_status_check
        CHECK (manual_status IN ('PENDING', 'CANCELLED')),
    CONSTRAINT installments_expected_amount_positive
        CHECK (expected_amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_inst_project_due
    ON installments (project_id, due_date);
CREATE INDEX IF NOT EXISTS idx_inst_customer
    ON installments (customer_id);
CREATE INDEX IF NOT EXISTS idx_inst_owner
    ON installments (created_by_user_id);

-- -----------------------------------------------------------------------------
-- Transactions: nullable link to an installment. Set ONLY for type=INCOME and
-- only when the installment belongs to the same project. Enforced in
-- TransactionService, not via a CHECK, so the user gets a friendly 400 message.
-- -----------------------------------------------------------------------------
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS linked_installment_id UUID;

-- Hot index for the "received against installment X" sum that drives
-- received_amount / remaining_amount derivation.
CREATE INDEX IF NOT EXISTS idx_txn_linked_installment
    ON transactions (linked_installment_id)
    WHERE linked_installment_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- Reminders: nullable link to an installment, mirroring the existing
-- linked_transaction_id / linked_project_id / linked_counterparty_name fields.
-- -----------------------------------------------------------------------------
ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS linked_installment_id UUID;

CREATE INDEX IF NOT EXISTS idx_rem_owner_installment
    ON reminders (created_by_user_id, linked_installment_id)
    WHERE linked_installment_id IS NOT NULL;
