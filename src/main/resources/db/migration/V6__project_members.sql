-- =============================================================================
-- V6 — Project membership (basic multi-user collaboration)
-- =============================================================================
-- Adds ONE table: project_members. Replaces the implicit "creator owns
-- everything" rule with explicit, role-based, soft-archivable membership.
--
-- Critical contracts:
--   - Access to a project = "row exists in project_members WHERE
--     project_id = ? AND user_id = ? AND archived_at IS NULL".
--   - projects.created_by is retained as historical/audit metadata only.
--     It is NOT used for authorization decisions after this migration.
--   - Role hierarchy: OWNER > EDITOR > ACCOUNTANT > VIEWER. Capability
--     checks happen at the service layer; the DB only stores the role.
--   - One row per (project, user). Re-inviting an archived member must
--     restore + update role rather than insert a duplicate. The UNIQUE
--     constraint enforces this at the data layer.
--   - Backfill: every existing project's creator becomes its sole OWNER
--     so no one loses access.
-- =============================================================================

CREATE TABLE IF NOT EXISTS project_members (
    id                      UUID PRIMARY KEY,
    project_id              UUID            NOT NULL,
    user_id                 UUID            NOT NULL,
    role                    VARCHAR(20)     NOT NULL,
    invited_by_user_id      UUID,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    archived_at             TIMESTAMPTZ,

    CONSTRAINT fk_pm_project       FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_pm_user          FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_pm_invited_by    FOREIGN KEY (invited_by_user_id) REFERENCES users(id),
    CONSTRAINT uq_pm_project_user  UNIQUE (project_id, user_id),
    CONSTRAINT ck_pm_role          CHECK (role IN ('OWNER','EDITOR','ACCOUNTANT','VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_pm_user_active
    ON project_members(user_id) WHERE archived_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_pm_project_active
    ON project_members(project_id) WHERE archived_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_pm_project_owner_active
    ON project_members(project_id) WHERE archived_at IS NULL AND role = 'OWNER';

-- Backfill: every existing project's creator becomes its OWNER member.
-- WHERE NOT EXISTS makes the migration safely idempotent.
INSERT INTO project_members (id, project_id, user_id, role, invited_by_user_id, created_at)
SELECT gen_random_uuid(), p.id, p.created_by, 'OWNER', NULL, p.created_at
FROM projects p
WHERE NOT EXISTS (
    SELECT 1 FROM project_members pm
    WHERE pm.project_id = p.id AND pm.user_id = p.created_by
);
