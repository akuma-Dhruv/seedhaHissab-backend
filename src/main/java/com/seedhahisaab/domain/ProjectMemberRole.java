package com.seedhahisaab.domain;

/**
 * Project membership roles in v1 of the collaboration model.
 *
 * <p>Capabilities (enforced ONLY by {@link com.seedhahisaab.service.ProjectAccessService}):
 * <ul>
 *   <li>{@link #OWNER} — full access, manages members and roles, edits transactions.</li>
 *   <li>{@link #EDITOR} — can view, create, and edit transactions.</li>
 *   <li>{@link #ACCOUNTANT} — can view and create transactions, but cannot edit.</li>
 *   <li>{@link #VIEWER} — read-only.</li>
 * </ul>
 *
 * <p>Hierarchy ordinal — internal-only, never serialized — provides a single
 * comparison rule so helpers don't sprawl into per-role switches:
 * <pre>
 *   OWNER (4) > EDITOR (3) > ACCOUNTANT (2) > VIEWER (1)
 * </pre>
 * Note that this hierarchy is for "is at least as privileged as" comparisons
 * and is NOT a clean substitute for capability checks like "can edit". E.g.
 * ACCOUNTANT > VIEWER but ACCOUNTANT cannot edit transactions while EDITOR
 * can. Use the dedicated {@code can*} predicates for those.
 */
public enum ProjectMemberRole {
    VIEWER(1),
    ACCOUNTANT(2),
    EDITOR(3),
    OWNER(4);

    private final int rank;

    ProjectMemberRole(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean atLeast(ProjectMemberRole other) {
        return this.rank >= other.rank;
    }

    public boolean canCreateTransaction() {
        return this == OWNER || this == EDITOR || this == ACCOUNTANT;
    }

    public boolean canEditTransaction() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canManageMembers() {
        return this == OWNER;
    }
}
