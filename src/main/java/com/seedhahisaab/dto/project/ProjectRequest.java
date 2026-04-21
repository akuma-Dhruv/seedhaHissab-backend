package com.seedhahisaab.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private BigDecimal plannedBudget;
    private BigDecimal expectedRevenue;
}
