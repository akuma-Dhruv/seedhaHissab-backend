-- =============================================================================
-- V1 — Baseline schema for SeedhaHisaab
-- =============================================================================
-- This file represents the schema as it stood when Flyway was first adopted.
-- For any pre-existing database it is skipped (Flyway baselines at V1); on a
-- fresh empty database it creates the full initial schema.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS projects (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    planned_budget    NUMERIC,
    expected_revenue  NUMERIC,
    created_by        UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_projects_created_by ON projects (created_by);

CREATE TABLE IF NOT EXISTS partners (
    id                UUID PRIMARY KEY,
    project_id        UUID         NOT NULL,
    name              VARCHAR(255) NOT NULL,
    share_percentage  NUMERIC      NOT NULL,
    user_id           UUID
);
CREATE INDEX IF NOT EXISTS idx_partners_project_id ON partners (project_id);

CREATE TABLE IF NOT EXISTS vendors (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    contact_info  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS transactions (
    id                    UUID PRIMARY KEY,
    root_transaction_id   UUID         NOT NULL,
    version               INTEGER      NOT NULL,
    previous_version_id   UUID,
    type                  VARCHAR(255) NOT NULL,
    amount                NUMERIC(19,4) NOT NULL,
    project_id            UUID,
    vendor_id             UUID,
    partner_id            UUID,
    paid_by_partner_id    UUID,
    owner_user_id         UUID,
    counterparty_name     VARCHAR(255),
    counterparty_user_id  UUID,
    purpose               VARCHAR(255),
    transaction_date      DATE         NOT NULL,
    status                VARCHAR(255) NOT NULL,
    created_by            UUID         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT transactions_status_check
        CHECK (status IN ('ACTIVE', 'OMITTED'))
);

CREATE INDEX IF NOT EXISTS idx_txn_project_id          ON transactions (project_id);
CREATE INDEX IF NOT EXISTS idx_txn_vendor_project      ON transactions (vendor_id, project_id);
CREATE INDEX IF NOT EXISTS idx_txn_paid_by_partner     ON transactions (paid_by_partner_id);
CREATE INDEX IF NOT EXISTS idx_txn_root_id             ON transactions (root_transaction_id);
CREATE INDEX IF NOT EXISTS idx_txn_root_version        ON transactions (root_transaction_id, version);
CREATE INDEX IF NOT EXISTS idx_txn_owner_user          ON transactions (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_txn_owner_counterparty  ON transactions (owner_user_id, counterparty_name);

-- Logical version uniqueness: a (root, version) pair must never repeat.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_txn_root_version'
    ) THEN
        ALTER TABLE transactions
            ADD CONSTRAINT uk_txn_root_version UNIQUE (root_transaction_id, version);
    END IF;
END $$;
