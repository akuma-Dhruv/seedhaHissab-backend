package com.seedhahisaab.service;

import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.summary.PersonalSummaryResponse;
import com.seedhahisaab.dto.transaction.PersonalTransactionRequest;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        validatePersonalType(req.getType());
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
        BigDecimal net = income.subtract(expense);
        return new PersonalSummaryResponse(userId, income, expense, net);
    }

    /**
     * Personal transactions only allow EXPENSE ("I paid") and INCOME ("I received").
     * Other transaction types belong to the project flow exclusively.
     */
    private void validatePersonalType(TransactionType type) {
        if (type != TransactionType.EXPENSE && type != TransactionType.INCOME) {
            throw ApiException.badRequest(
                    "Personal transactions only support EXPENSE or INCOME (got " + type + ")");
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
