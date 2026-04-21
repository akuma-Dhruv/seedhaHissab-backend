package com.seedhahisaab.dto.transaction;

import com.seedhahisaab.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private UUID projectId;
    private UUID vendorId;
    private UUID partnerId;
    private UUID paidByPartnerId;

    private String purpose;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
}
