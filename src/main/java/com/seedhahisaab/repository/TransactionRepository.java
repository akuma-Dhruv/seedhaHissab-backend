package com.seedhahisaab.repository;

import com.seedhahisaab.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query(value = """
            SELECT t.* FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
            ORDER BY t.transaction_date DESC, t.created_at DESC
            LIMIT :lim OFFSET :off
            """, nativeQuery = true)
    List<Transaction> findLatestByProject(
            @Param("projectId") UUID projectId,
            @Param("includeOmitted") boolean includeOmitted,
            @Param("lim") int limit,
            @Param("off") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
            """, nativeQuery = true)
    long countLatestByProject(
            @Param("projectId") UUID projectId,
            @Param("includeOmitted") boolean includeOmitted);

    @Query(value = """
            SELECT t.* FROM transactions t
            WHERE t.root_transaction_id = :rootId
            ORDER BY t.version ASC
            """, nativeQuery = true)
    List<Transaction> findAllVersionsByRootId(@Param("rootId") UUID rootId);

    @Query(value = """
            SELECT t.* FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE root_transaction_id = :rootId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            """, nativeQuery = true)
    Optional<Transaction> findLatestByRootId(@Param("rootId") UUID rootId);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = :txnType
            """, nativeQuery = true)
    BigDecimal sumActiveByProjectAndType(
            @Param("projectId") UUID projectId,
            @Param("txnType") String txnType);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = :txnType
              AND t.paid_by_partner_id = :partnerId
            """, nativeQuery = true)
    BigDecimal sumActiveByProjectTypeAndPaidBy(
            @Param("projectId") UUID projectId,
            @Param("txnType") String txnType,
            @Param("partnerId") UUID partnerId);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = :txnType
              AND t.partner_id = :partnerId
            """, nativeQuery = true)
    BigDecimal sumActiveByProjectTypeAndPartner(
            @Param("projectId") UUID projectId,
            @Param("txnType") String txnType,
            @Param("partnerId") UUID partnerId);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE project_id = :projectId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = :txnType
              AND t.vendor_id = :vendorId
            """, nativeQuery = true)
    BigDecimal sumActiveByProjectVendorAndType(
            @Param("projectId") UUID projectId,
            @Param("vendorId") UUID vendorId,
            @Param("txnType") String txnType);

    // ----------------------------------------------------------------------
    // Personal-transaction queries
    // Personal transactions are rows where project_id IS NULL and the
    // owner_user_id matches the current user. Same latest-version-wins +
    // omit-aware semantics as the project-scoped queries above.
    // ----------------------------------------------------------------------

    @Query(value = """
            SELECT t.* FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
            ORDER BY t.transaction_date DESC, t.created_at DESC
            LIMIT :lim OFFSET :off
            """, nativeQuery = true)
    List<Transaction> findLatestPersonalByOwner(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("includeOmitted") boolean includeOmitted,
            @Param("lim") int limit,
            @Param("off") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
            """, nativeQuery = true)
    long countLatestPersonalByOwner(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("includeOmitted") boolean includeOmitted);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = :txnType
            """, nativeQuery = true)
    BigDecimal sumActivePersonalByOwnerAndType(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("txnType") String txnType);

    // ----------------------------------------------------------------------
    // Counterparty ledger queries
    //
    // Aggregates personal latest-active rows by lower-trimmed counterparty
    // name. The "given" / "received" columns sum signed amounts according to
    // the convention in PersonalLedgerSign:
    //   given    = SUM(amount) where type IN (LEND, REPAYMENT_GIVEN, EXPENSE)
    //   received = SUM(amount) where type IN (BORROW, REPAYMENT_RECEIVED, INCOME)
    // EXPENSE / INCOME without a counterparty are excluded by the
    // counterparty_name IS NOT NULL filter.
    //
    // Native query returns Object[]: { counterpartyName, given, received }.
    // ----------------------------------------------------------------------

    @Query(value = """
            SELECT
                MAX(t.counterparty_name) AS counterparty_name,
                COALESCE(SUM(CASE WHEN t.type IN ('LEND','REPAYMENT_GIVEN','EXPENSE') THEN t.amount ELSE 0 END), 0) AS total_given,
                COALESCE(SUM(CASE WHEN t.type IN ('BORROW','REPAYMENT_RECEIVED','INCOME') THEN t.amount ELSE 0 END), 0) AS total_received
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.counterparty_name IS NOT NULL
              AND TRIM(t.counterparty_name) <> ''
              AND (CAST(:search AS text) IS NULL
                   OR LOWER(t.counterparty_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            GROUP BY LOWER(TRIM(t.counterparty_name))
            ORDER BY MAX(t.counterparty_name) ASC
            LIMIT :lim OFFSET :off
            """, nativeQuery = true)
    List<Object[]> aggregateCounterpartiesPaged(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("search") String search,
            @Param("lim") int limit,
            @Param("off") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM transactions t
                INNER JOIN (
                    SELECT root_transaction_id, MAX(version) AS max_version
                    FROM transactions
                    WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                    GROUP BY root_transaction_id
                ) latest ON t.root_transaction_id = latest.root_transaction_id
                        AND t.version = latest.max_version
                WHERE t.status = 'ACTIVE'
                  AND t.counterparty_name IS NOT NULL
                  AND TRIM(t.counterparty_name) <> ''
                  AND (CAST(:search AS text) IS NULL
                       OR LOWER(t.counterparty_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
                GROUP BY LOWER(TRIM(t.counterparty_name))
            ) grouped
            """, nativeQuery = true)
    long countCounterparties(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("search") String search);

    /**
     * Returns one row per counterparty (no pagination, no search) for
     * computing roll-up totals like totalReceivable / totalPayable.
     * Counterparty count is expected to be small (tens, not thousands).
     */
    @Query(value = """
            SELECT
                MAX(t.counterparty_name) AS counterparty_name,
                COALESCE(SUM(CASE WHEN t.type IN ('LEND','REPAYMENT_GIVEN','EXPENSE') THEN t.amount ELSE 0 END), 0) AS total_given,
                COALESCE(SUM(CASE WHEN t.type IN ('BORROW','REPAYMENT_RECEIVED','INCOME') THEN t.amount ELSE 0 END), 0) AS total_received
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.counterparty_name IS NOT NULL
              AND TRIM(t.counterparty_name) <> ''
            GROUP BY LOWER(TRIM(t.counterparty_name))
            """, nativeQuery = true)
    List<Object[]> aggregateAllCounterparties(@Param("ownerUserId") UUID ownerUserId);

    /**
     * All latest-version personal rows for a single counterparty (case- and
     * trim-insensitive match), ordered newest-first. Used by the
     * /personal/counterparties/{name}/ledger endpoint.
     */
    @Query(value = """
            SELECT t.* FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
              AND t.counterparty_name IS NOT NULL
              AND LOWER(TRIM(t.counterparty_name)) = LOWER(TRIM(CAST(:counterpartyName AS text)))
            ORDER BY t.transaction_date DESC, t.created_at DESC
            LIMIT :lim OFFSET :off
            """, nativeQuery = true)
    List<Transaction> findCounterpartyLedger(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("counterpartyName") String counterpartyName,
            @Param("includeOmitted") boolean includeOmitted,
            @Param("lim") int limit,
            @Param("off") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE owner_user_id = :ownerUserId AND project_id IS NULL
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE (:includeOmitted = true OR t.status = 'ACTIVE')
              AND t.counterparty_name IS NOT NULL
              AND LOWER(TRIM(t.counterparty_name)) = LOWER(TRIM(CAST(:counterpartyName AS text)))
            """, nativeQuery = true)
    long countCounterpartyLedger(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("counterpartyName") String counterpartyName,
            @Param("includeOmitted") boolean includeOmitted);
}
