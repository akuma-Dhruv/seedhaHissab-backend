package com.seedhahisaab.dto.transaction;

import com.seedhahisaab.domain.TransactionStatus;
import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionResponse {
    private UUID id;
    private UUID rootTransactionId;
    private int version;
    private UUID previousVersionId;
    private TransactionType type;
    private BigDecimal amount;
    private UUID projectId;
    private UUID vendorId;
    private UUID partnerId;
    private UUID paidByPartnerId;
    private UUID linkedInstallmentId;
    private UUID ownerUserId;
    private String counterpartyName;
    private UUID counterpartyUserId;
    private String purpose;
    private LocalDate transactionDate;
    private TransactionStatus status;
    private UUID createdBy;
    private Instant createdAt;

    public static TransactionResponse from(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.setId(t.getId());
        r.setRootTransactionId(t.getRootTransactionId());
        r.setVersion(t.getVersion());
        r.setPreviousVersionId(t.getPreviousVersionId());
        r.setType(t.getType());
        r.setAmount(t.getAmount());
        r.setProjectId(t.getProjectId());
        r.setVendorId(t.getVendorId());
        r.setPartnerId(t.getPartnerId());
        r.setPaidByPartnerId(t.getPaidByPartnerId());
        r.setLinkedInstallmentId(t.getLinkedInstallmentId());
        r.setOwnerUserId(t.getOwnerUserId());
        r.setCounterpartyName(t.getCounterpartyName());
        r.setCounterpartyUserId(t.getCounterpartyUserId());
        r.setPurpose(t.getPurpose());
        r.setTransactionDate(t.getTransactionDate());
        r.setStatus(t.getStatus());
        r.setCreatedBy(t.getCreatedBy());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}
