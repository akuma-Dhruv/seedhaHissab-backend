-- =============================================================================
-- V2 — Personal ledger constraint clean-up
-- =============================================================================
-- The earlier (Hibernate-managed) schema in some environments shipped with a
-- restrictive CHECK constraint on transactions.type that hard-coded the old
-- enum members and rejected the new personal-ledger types
-- (LEND, BORROW, REPAYMENT_GIVEN, REPAYMENT_RECEIVED).
--
-- This migration drops that legacy CHECK if it still exists, and re-asserts
-- the (root_transaction_id, version) uniqueness constraint that the
-- versioning engine depends on. Both steps are idempotent.
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'transactions_type_check'
    ) THEN
        ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_txn_root_version'
    ) THEN
        ALTER TABLE transactions
            ADD CONSTRAINT uk_txn_root_version UNIQUE (root_transaction_id, version);
    END IF;
END $$;
