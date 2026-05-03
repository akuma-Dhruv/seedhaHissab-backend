package com.seedhahisaab.service;

import com.seedhahisaab.domain.PersonalLedgerSign;
import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.summary.CounterpartySummaryResponse;
import com.seedhahisaab.dto.summary.PersonalSummaryResponse;
import com.seedhahisaab.dto.transaction.PersonalTransactionRequest;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for PERSONAL (person-to-person) transactions.
 *
 * Personal transactions reuse the same `transactions` table and the same
 * versioning + omit engine as project transactions, but are distinguished by
 * having `project_id IS NULL` and `owner_user_id = <current user>`.
 *
 * Project-transaction logic in {@link TransactionService} is intentionally
 * NOT modified by this class — the two flows live side-by-side and only
 * share the underlying repository.
 */
@Service
public class PersonalTransactionService {

    private final TransactionRepository transactionRepository;

    public PersonalTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse create(PersonalTransactionRequest req, UUID userId) {
        validatePersonalRequest(req);
        UUID id = UUID.randomUUID();
        Transaction txn = Transaction.builder()
                .id(id)
                .rootTransactionId(id)
                .version(1)
                .previousVersionId(null)
                .type(req.getType())
                .amount(req.getAmount())
                // Personal transactions are NEVER tied to a project / vendor / partner.
                .projectId(null)
                .vendorId(null)
                .partnerId(null)
                .paidByPartnerId(null)
                .ownerUserId(userId)
                .counterpartyName(trimToNull(req.getCounterpartyName()))
                .counterpartyUserId(null)
                .purpose(trimToNull(req.getPurpose()))
                .transactionDate(req.getTransactionDate())
                .status(TransactionStatus.ACTIVE)
                .createdBy(userId)
                .build();
        return TransactionResponse.from(transactionRepository.save(txn));
    }

    /**
     * Edits a personal transaction by appending a new version.
     *
     * - Resolves the latest version for the row's root.
     * - Authorizes that the owner matches the JWT user and projectId is NULL.
     * - Refuses to edit an OMITTED chain.
     * - Inserts a new ACTIVE row with version + 1; the old row is untouched.
     *
     * Editable fields: type, amount, purpose, transactionDate, counterpartyName.
     * Non-editable (carried from previous version): ownerUserId, counterpartyUserId.
     * Forbidden (always NULL on personal): projectId, vendorId, partnerId, paidByPartnerId.
     */
    @Transactional
    public TransactionResponse update(UUID txnId, PersonalTransactionRequest req, UUID userId) {
        validatePersonalRequest(req);
        Transaction byId = transactionRepository.findById(txnId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        Transaction prev = transactionRepository.findLatestByRootId(byId.getRootTransactionId())
                .orElseThrow(() -> ApiException.notFound("No active version found for transaction"));

        if (prev.getProjectId() != null) {
            throw ApiException.badRequest("Project transactions cannot be edited via the personal endpoint");
        }
        if (prev.getOwnerUserId() == null || !prev.getOwnerUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        if (prev.getStatus() == TransactionStatus.OMITTED) {
            throw ApiException.badRequest("Cannot edit an omitted transaction");
        }

        Transaction next = Transaction.builder()
                .id(UUID.randomUUID())
                .rootTransactionId(prev.getRootTransactionId())
                .version(prev.getVersion() + 1)
                .previousVersionId(prev.getId())
                .type(req.getType())
                .amount(req.getAmount())
                .projectId(null)
                .vendorId(null)
                .partnerId(null)
                .paidByPartnerId(null)
                .ownerUserId(userId)
                .counterpartyName(trimToNull(req.getCounterpartyName()))
                .counterpartyUserId(prev.getCounterpartyUserId())
                .purpose(trimToNull(req.getPurpose()))
                .transactionDate(req.getTransactionDate())
                .status(TransactionStatus.ACTIVE)
                .createdBy(userId)
                .build();
        return TransactionResponse.from(transactionRepository.save(next));
    }

    public PagedResponse<TransactionResponse> list(UUID userId, boolean includeOmitted, int page, int limit) {
        int offset = page * limit;
        List<TransactionResponse> data = transactionRepository
                .findLatestPersonalByOwner(userId, includeOmitted, limit, offset)
                .stream().map(TransactionResponse::from).collect(Collectors.toList());
        long total = transactionRepository.countLatestPersonalByOwner(userId, includeOmitted);
        return new PagedResponse<>(data, page, limit, total);
    }

    public PersonalSummaryResponse summary(UUID userId) {
        BigDecimal income = orZero(transactionRepository
                .sumActivePersonalByOwnerAndType(userId, TransactionType.INCOME.name()));
        BigDecimal expense = orZero(transactionRepository
                .sumActivePersonalByOwnerAndType(userId, TransactionType.EXPENSE.name()));
        BigDecimal lent = orZero(transactionRepository
                .sumActivePersonalByOwnerAndType(userId, TransactionType.LEND.name()));
        BigDecimal borrowed = orZero(transactionRepository
                .sumActivePersonalByOwnerAndType(userId, TransactionType.BORROW.name()));

        // Roll up per-counterparty net balances into receivable / payable totals.
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal payable = BigDecimal.ZERO;
        for (Object[] row : transactionRepository.aggregateAllCounterparties(userId)) {
            BigDecimal given = orZero((BigDecimal) row[1]);
            BigDecimal received = orZero((BigDecimal) row[2]);
            BigDecimal balance = given.subtract(received);
            int s = balance.signum();
            if (s > 0) receivable = receivable.add(balance);
            else if (s < 0) payable = payable.add(balance.abs());
        }

        BigDecimal net = receivable.subtract(payable);
        return new PersonalSummaryResponse(userId, income, expense, lent, borrowed, receivable, payable, net);
    }

    public PagedResponse<CounterpartySummaryResponse> counterparties(UUID userId, String search,
                                                                      int page, int limit) {
        int offset = page * limit;
        String trimmedSearch = search == null ? null : search.trim();
        if (trimmedSearch != null && trimmedSearch.isEmpty()) trimmedSearch = null;

        List<Object[]> rows = transactionRepository.aggregateCounterpartiesPaged(
                userId, trimmedSearch, limit, offset);
        List<CounterpartySummaryResponse> data = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String name = (String) row[0];
            BigDecimal given = orZero((BigDecimal) row[1]);
            BigDecimal received = orZero((BigDecimal) row[2]);
            BigDecimal balance = given.subtract(received);
            data.add(new CounterpartySummaryResponse(
                    name, given, received, balance, PersonalLedgerSign.direction(balance)));
        }
        long total = transactionRepository.countCounterparties(userId, trimmedSearch);
        return new PagedResponse<>(data, page, limit, total);
    }

    public PagedResponse<TransactionResponse> counterpartyLedger(UUID userId, String counterpartyName,
                                                                  boolean includeOmitted, int page, int limit) {
        if (counterpartyName == null || counterpartyName.trim().isEmpty()) {
            throw ApiException.badRequest("Counterparty name is required");
        }
        int offset = page * limit;
        List<TransactionResponse> data = transactionRepository
                .findCounterpartyLedger(userId, counterpartyName, includeOmitted, limit, offset)
                .stream().map(TransactionResponse::from).collect(Collectors.toList());
        long total = transactionRepository.countCounterpartyLedger(userId, counterpartyName, includeOmitted);
        return new PagedResponse<>(data, page, limit, total);
    }

    /**
     * Validates the cross-field rules for a personal-transaction request:
     *  - type must be in the personal allow-list
     *  - LEND / BORROW / REPAYMENT_* require a non-blank counterpartyName
     */
    private void validatePersonalRequest(PersonalTransactionRequest req) {
        TransactionType type = req.getType();
        if (!PersonalLedgerSign.ALLOWED_PERSONAL_TYPES.contains(type)) {
            throw ApiException.badRequest(
                    "Personal transactions only support EXPENSE, INCOME, LEND, BORROW, " +
                            "REPAYMENT_GIVEN, REPAYMENT_RECEIVED (got " + type + ")");
        }
        String cp = trimToNull(req.getCounterpartyName());
        if (PersonalLedgerSign.COUNTERPARTY_REQUIRED_TYPES.contains(type) && cp == null) {
            throw ApiException.badRequest(
                    "Counterparty name is required for " + type + " transactions");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
