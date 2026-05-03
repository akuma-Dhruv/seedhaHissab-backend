package com.seedhahisaab.dto.hidden;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body for {@code POST /projects/{projectId}/hidden-partners}. The share
 * percentage is a percentage of the chosen official partner's slice — never
 * a percentage of the total project.
 */
@Data
public class HiddenPartnerAgreementRequest {

    @NotNull(message = "Official partner is required")
    private UUID officialPartnerId;

    @NotBlank(message = "Hidden partner name is required")
    @Size(max = 255, message = "Name must be 255 characters or fewer")
    private String hiddenPartnerName;

    @NotNull(message = "Share percentage is required")
    @DecimalMin(value = "0.01", message = "Share must be greater than 0")
    @DecimalMax(value = "100.00", message = "Share cannot exceed 100")
    private BigDecimal sharePercentage;

    @Size(max = 2048, message = "Notes must be 2048 characters or fewer")
    private String notes;
}
