package com.seedhahisaab.dto.installment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body for {@code POST /projects/{projectId}/installments} and
 * {@code PUT /installments/{id}}. The expected amount is planning metadata
 * (the promise) — actual money received still comes through INCOME
 * transactions linked via {@code linkedInstallmentId}.
 */
@Data
public class InstallmentRequest {

    @NotNull(message = "Customer is required")
    private UUID customerId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or fewer")
    private String title;

    @Size(max = 1024, message = "Description must be 1024 characters or fewer")
    private String description;

    @NotNull(message = "Expected amount is required")
    @DecimalMin(value = "0.01", message = "Expected amount must be greater than 0")
    private BigDecimal expectedAmount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
