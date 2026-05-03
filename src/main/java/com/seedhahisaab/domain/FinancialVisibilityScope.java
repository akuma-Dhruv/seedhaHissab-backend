package com.seedhahisaab.domain;

/**
 * Lightweight visibility marker for financial data surfaced in DTOs.
 *
 * <p>Today only two scopes exist:
 * <ul>
 *   <li>{@link #OFFICIAL} — visible to anyone with project access. Project
 *       summary, partner settlements, transactions, installments.</li>
 *   <li>{@link #PRIVATE} — visible only to the creator of the underlying
 *       record. Hidden partner agreements and their derived settlements.</li>
 * </ul>
 *
 * <p>The enum exists so future visibility layers (per-partner private notes,
 * shared-but-restricted reports, etc.) can be added additively without
 * threading new boolean flags through every DTO.
 */
public enum FinancialVisibilityScope {
    OFFICIAL,
    PRIVATE
}
