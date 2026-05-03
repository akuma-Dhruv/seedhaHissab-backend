-- =============================================================================
-- V5 — Hidden Partner & Internal Settlement Engine
-- =============================================================================
-- Adds ONE new table. Zero ALTERs to partners/transactions/projects/installments.
--
-- Hidden partner agreements are a *private overlay* on official partners.
-- They never affect project summaries, official partner settlements, or
-- the receivable engine. Calculations are derived at read-time:
--
--     Project -> Official Partner -> Hidden Partner
--
-- The official partner's allocation comes from PartnerSettlementService
-- (untouched). HiddenSettlementService takes that allocation and splits it
-- per agreement using share_percentage as a percentage of the OFFICIAL
-- PARTNER'S SLICE (not of the total project).
--
-- Privacy rule: rows are visible only to created_by_user_id.
-- =============================================================================

CREATE TABLE IF NOT EXISTS hidden_partner_agreements (
    id                          UUID PRIMARY KEY,
    project_id                  UUID            NOT NULL,
    official_partner_id         UUID            NOT NULL,
    hidden_partner_name         VARCHAR(255)    NOT NULL,
    hidden_partner_user_id      UUID,
    share_percentage            NUMERIC(5,2)    NOT NULL,
    notes                       VARCHAR(2048),
    created_by_user_id          UUID            NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL,
    updated_at                  TIMESTAMPTZ     NOT NULL,
    archived_at                 TIMESTAMPTZ,
    CONSTRAINT hpa_share_range
        CHECK (share_percentage > 0 AND share_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_hpa_project
    ON hidden_partner_agreements (project_id);

CREATE INDEX IF NOT EXISTS idx_hpa_official_partner
    ON hidden_partner_agreements (official_partner_id);

-- Hot path: "list my active agreements on this project"
CREATE INDEX IF NOT EXISTS idx_hpa_owner_archived
    ON hidden_partner_agreements (created_by_user_id, archived_at);
