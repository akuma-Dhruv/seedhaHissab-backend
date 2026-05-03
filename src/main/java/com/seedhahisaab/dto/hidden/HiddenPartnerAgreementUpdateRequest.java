package com.seedhahisaab.dto.hidden;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Body for {@code PATCH /hidden-partners/{id}}. All fields optional. Only
 * {@code sharePercentage} and {@code notes} are mutable — name and the
 * official-partner pairing are intentionally fixed for audit clarity.
 */
@Data
public class HiddenPartnerAgreementUpdateRequest {

    @DecimalMin(value = "0.01", message = "Share must be greater than 0")
    @DecimalMax(value = "100.00", message = "Share cannot exceed 100")
    private BigDecimal sharePercentage;

    @Size(max = 2048, message = "Notes must be 2048 characters or fewer")
    private String notes;
}
