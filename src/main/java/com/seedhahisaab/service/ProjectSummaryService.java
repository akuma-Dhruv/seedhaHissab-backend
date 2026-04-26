package com.seedhahisaab.service;

import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.summary.ProjectSummaryResponse;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProjectSummaryService {

    private final TransactionRepository transactionRepository;
    private final ProjectService projectService;

    public ProjectSummaryService(TransactionRepository transactionRepository, ProjectService projectService) {
        this.transactionRepository = transactionRepository;
        this.projectService = projectService;
    }

    public ProjectSummaryResponse getSummary(UUID projectId, UUID userId) {
        projectService.requireProject(projectId, userId);

        BigDecimal totalIncome = orZero(transactionRepository.sumActiveByProjectAndType(
                projectId, TransactionType.INCOME.name()));
        BigDecimal totalExpense = orZero(transactionRepository.sumActiveByProjectAndType(
                projectId, TransactionType.EXPENSE.name()));
        BigDecimal profit = totalIncome.subtract(totalExpense);

        return new ProjectSummaryResponse(projectId, totalIncome, totalExpense, profit);
    }

    private BigDecimal orZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
