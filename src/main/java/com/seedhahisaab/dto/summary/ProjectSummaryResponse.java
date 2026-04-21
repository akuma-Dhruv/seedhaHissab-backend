package com.seedhahisaab.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ProjectSummaryResponse {
    private UUID projectId;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal profit;
}
