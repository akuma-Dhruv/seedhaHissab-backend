package com.seedhahisaab.dto.partner;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PartnerRequest {

    @NotBlank(message = "Partner name is required")
    private String name;

    @NotNull(message = "Share percentage is required")
    @DecimalMin(value = "0.00", message = "Share percentage must be >= 0")
    @DecimalMax(value = "100.00", message = "Share percentage must be <= 100")
    private BigDecimal sharePercentage;

    private UUID userId;
}
