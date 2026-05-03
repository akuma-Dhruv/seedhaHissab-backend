package com.seedhahisaab.repository;

import com.seedhahisaab.domain.InstallmentManualStatus;
import com.seedhahisaab.entity.Installment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Installment}. The financial sum queries
 * here only count <em>latest-version, ACTIVE, INCOME</em> transactions —
 * matching the same rules the rest of the project ledger uses, so editing
 * or omitting an income transaction automatically updates the installment's
 * derived received amount.
 */
@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    Optional<Installment> findByIdAndCreatedByUserId(UUID id, UUID userId);

    @Query("""
            SELECT i FROM Installment i
            WHERE i.projectId = :projectId
              AND (:manualStatus IS NULL OR i.manualStatus = :manualStatus)
              AND (:customerId IS NULL OR i.customerId = :customerId)
            ORDER BY i.dueDate ASC, i.createdAt ASC
            """)
    Page<Installment> searchByProject(
            @Param("projectId") UUID projectId,
            @Param("manualStatus") InstallmentManualStatus manualStatus,
            @Param("customerId") UUID customerId,
            Pageable pageable);

    @Query("SELECT i FROM Installment i WHERE i.projectId = :projectId ORDER BY i.dueDate ASC, i.createdAt ASC")
    List<Installment> findAllByProject(@Param("projectId") UUID projectId);

    /**
     * Sum of latest-version, ACTIVE, INCOME transactions linked to the given
     * installment. Returns 0 when nothing is linked. Used by the derivation
     * helper to compute receivedAmount.
     */
    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE linked_installment_id = :installmentId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = 'INCOME'
              AND t.linked_installment_id = :installmentId
            """, nativeQuery = true)
    BigDecimal sumReceivedForInstallment(@Param("installmentId") UUID installmentId);

    /**
     * Returns the latest, ACTIVE INCOME transactions linked to the given
     * installment, newest-first. Drives the payment-history list on the
     * installment detail page.
     */
    @Query(value = """
            SELECT t.* FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE linked_installment_id = :installmentId
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = 'INCOME'
              AND t.linked_installment_id = :installmentId
            ORDER BY t.transaction_date DESC, t.created_at DESC
            """, nativeQuery = true)
    List<com.seedhahisaab.entity.Transaction> findLinkedPaymentsForInstallment(
            @Param("installmentId") UUID installmentId);

    /**
     * Aggregate received-amount for a batch of installment ids in a single
     * query. Returns rows of {@code [installmentId UUID, sum NUMERIC]}; ids
     * that have no payments are omitted, callers default them to 0.
     */
    @Query(value = """
            SELECT t.linked_installment_id, COALESCE(SUM(t.amount), 0)
            FROM transactions t
            INNER JOIN (
                SELECT root_transaction_id, MAX(version) AS max_version
                FROM transactions
                WHERE linked_installment_id IN (:installmentIds)
                GROUP BY root_transaction_id
            ) latest ON t.root_transaction_id = latest.root_transaction_id
                    AND t.version = latest.max_version
            WHERE t.status = 'ACTIVE'
              AND t.type = 'INCOME'
              AND t.linked_installment_id IN (:installmentIds)
            GROUP BY t.linked_installment_id
            """, nativeQuery = true)
    List<Object[]> sumReceivedForInstallments(
            @Param("installmentIds") List<UUID> installmentIds);
}
