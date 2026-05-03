package com.seedhahisaab.dto.transaction;

import com.seedhahisaab.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or editing a personal transaction.
 *
 * The same shape is used for both POST /personal/transactions and
 * PUT /personal/transactions/{id}. Cross-field validation (e.g.
 * counterpartyName required for LEND/BORROW/REPAYMENT_*) lives in the
 * service so that error messages can reference the resolved type.
 */
@Data
public class PersonalTransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 255, message = "Counterparty name must be 255 characters or fewer")
    private String counterpartyName;

    @Size(max = 1024, message = "Purpose must be 1024 characters or fewer")
    private String purpose;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
}
