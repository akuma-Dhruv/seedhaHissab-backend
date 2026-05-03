package com.seedhahisaab.service;

import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.transaction.TransactionRequest;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.entity.Transaction;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;
    private final InstallmentService installmentService;

    public TransactionService(TransactionRepository transactionRepository,
                               ProjectService projectService,
                               ProjectAccessService projectAccessService,
                               InstallmentService installmentService) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
        this.installmentService = installmentService;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest req, UUID userId) {
        validate(req);
        if (req.getProjectId() != null) {
            // OWNER, EDITOR, ACCOUNTANT can record. VIEWER is rejected.
            projectAccessService.requireCanCreateTransaction(req.getProjectId(), userId);
        }
        UUID linkedInstallmentId = resolveLinkedInstallment(req, null, userId);
        UUID id = UUID.randomUUID();
        Transaction txn = Transaction.builder()
                .id(id)
                .rootTransactionId(id)
                .version(1)
                .previousVersionId(null)
                .type(req.getType())
                .amount(req.getAmount())
                .projectId(req.getProjectId())
                .vendorId(req.getVendorId())
                .partnerId(req.getPartnerId())
                .paidByPartnerId(req.getPaidByPartnerId())
                .linkedInstallmentId(linkedInstallmentId)
                .purpose(req.getPurpose())
                .transactionDate(req.getTransactionDate())
                .status(TransactionStatus.ACTIVE)
                .createdBy(userId)
                .build();
        return TransactionResponse.from(transactionRepository.save(txn));
    }

    @Transactional
    public TransactionResponse update(UUID txnId, TransactionRequest req, UUID userId) {
        validate(req);
        Transaction prev = resolveLatest(txnId);
        requireTransactionEdit(prev, userId);
        if (req.getProjectId() != null && !req.getProjectId().equals(prev.getProjectId())) {
            projectAccessService.requireCanEditTransaction(req.getProjectId(), userId);
        }
        UUID linkedInstallmentId = resolveLinkedInstallment(req, prev, userId);
        Transaction next = Transaction.builder()
                .id(UUID.randomUUID())
                .rootTransactionId(prev.getRootTransactionId())
                .version(prev.getVersion() + 1)
                .previousVersionId(prev.getId())
                .type(req.getType())
                .amount(req.getAmount())
                .projectId(req.getProjectId())
                .vendorId(req.getVendorId())
                .partnerId(req.getPartnerId())
                .paidByPartnerId(req.getPaidByPartnerId())
                .linkedInstallmentId(linkedInstallmentId)
                .purpose(req.getPurpose())
                .transactionDate(req.getTransactionDate())
                .status(TransactionStatus.ACTIVE)
                .createdBy(userId)
                .build();
        return TransactionResponse.from(transactionRepository.save(next));
    }

    @Transactional
    public TransactionResponse omit(UUID txnId, UUID userId) {
        Transaction prev = resolveLatest(txnId);
        requireTransactionEdit(prev, userId);
        Transaction omitted = Transaction.builder()
                .id(UUID.randomUUID())
                .rootTransactionId(prev.getRootTransactionId())
                .version(prev.getVersion() + 1)
                .previousVersionId(prev.getId())
                .type(prev.getType())
                .amount(prev.getAmount())
                .projectId(prev.getProjectId())
                .vendorId(prev.getVendorId())
                .partnerId(prev.getPartnerId())
                .paidByPartnerId(prev.getPaidByPartnerId())
                .linkedInstallmentId(prev.getLinkedInstallmentId())
                .ownerUserId(prev.getOwnerUserId())
                .counterpartyName(prev.getCounterpartyName())
                .counterpartyUserId(prev.getCounterpartyUserId())
                .purpose(prev.getPurpose())
                .transactionDate(prev.getTransactionDate())
                .status(TransactionStatus.OMITTED)
                .createdBy(userId)
                .build();
        return TransactionResponse.from(transactionRepository.save(omitted));
    }

    public PagedResponse<TransactionResponse> getByProject(UUID projectId, UUID userId,
                                                            boolean includeOmitted, int page, int limit) {
        projectService.requireProject(projectId, userId);
        int offset = page * limit;
        List<TransactionResponse> data = transactionRepository
                .findLatestByProject(projectId, includeOmitted, limit, offset)
                .stream().map(TransactionResponse::from).collect(Collectors.toList());
        long total = transactionRepository.countLatestByProject(projectId, includeOmitted);
        return new PagedResponse<>(data, page, limit, total);
    }

    public List<TransactionResponse> getHistory(UUID txnId, UUID userId) {
        Transaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        if (txn.getProjectId() != null) {
            projectService.requireProject(txn.getProjectId(), userId);
        } else {
            if (!txn.getCreatedBy().equals(userId)) {
                throw ApiException.forbidden("Access denied");
            }
        }
        return transactionRepository.findAllVersionsByRootId(txn.getRootTransactionId())
                .stream().map(TransactionResponse::from).collect(Collectors.toList());
    }

    private Transaction resolveLatest(UUID txnId) {
        Transaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        return transactionRepository.findLatestByRootId(txn.getRootTransactionId())
                .orElseThrow(() -> ApiException.notFound("No active version found for transaction"));
    }

    /**
     * Edit-capability gate for an existing transaction. Project transactions
     * require OWNER or EDITOR membership (ACCOUNTANT can create but not
     * edit). Personal-ledger transactions remain creator-only — sharing a
     * project never shares personal data.
     */
    private void requireTransactionEdit(Transaction txn, UUID userId) {
        if (txn.getProjectId() != null) {
            projectAccessService.requireCanEditTransaction(txn.getProjectId(), userId);
        } else if (!txn.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
    }

    /**
     * Resolves the {@code linkedInstallmentId} for a create/update operation.
     *
     * <ul>
     *   <li>If the caller explicitly passes a non-null id, validate it: must
     *       be INCOME, must belong to the same project, must not be cancelled.</li>
     *   <li>If the caller passes null and we have a previous version, carry
     *       forward the previous link unchanged. This is the "engine survives
     *       edits and omits" property documented in the receivable rules.</li>
     *   <li>For non-INCOME types, hard-strip the link — it has no meaning.</li>
     * </ul>
     */
    private UUID resolveLinkedInstallment(TransactionRequest req, Transaction prev, UUID userId) {
        // Only INCOME transactions may carry a link.
        if (req.getType() != TransactionType.INCOME) {
            if (req.getLinkedInstallmentId() != null) {
                throw ApiException.badRequest(
                        "Only INCOME transactions can be linked to an installment");
            }
            return null;
        }

        UUID candidate = req.getLinkedInstallmentId();
        if (candidate == null && prev != null) {
            // Carry forward unchanged — matches engine "survives edits/omits" rule.
            candidate = prev.getLinkedInstallmentId();
        }
        if (candidate == null) return null;

        installmentService.requireValidLinkTarget(candidate, req.getProjectId(), userId);
        return candidate;
    }

    private void validate(TransactionRequest req) {
        TransactionType type = req.getType();
        if (type == null) return;
        switch (type) {
            case EXPENSE -> {
                if (req.getPaidByPartnerId() == null) {
                    throw ApiException.badRequest("paidByPartnerId is required for EXPENSE transactions");
                }
            }
            case VENDOR_SUPPLY, VENDOR_PAYMENT -> {
                if (req.getVendorId() == null) {
                    throw ApiException.badRequest("vendorId is required for " + type + " transactions");
                }
            }
            case PROFIT_WITHDRAWAL -> {
                if (req.getPartnerId() == null) {
                    throw ApiException.badRequest("partnerId is required for PROFIT_WITHDRAWAL transactions");
                }
            }
            case INCOME -> {
                if (req.getProjectId() == null) {
                    throw ApiException.badRequest("projectId is required for INCOME transactions");
                }
            }
            default -> {
            }
        }
    }
}
