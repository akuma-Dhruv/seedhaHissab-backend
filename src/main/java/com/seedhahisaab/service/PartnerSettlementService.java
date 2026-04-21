package com.seedhahisaab.service;

import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.summary.PartnerSettlementResponse;
import com.seedhahisaab.entity.Partner;
import com.seedhahisaab.repository.PartnerRepository;
import com.seedhahisaab.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PartnerSettlementService {

    private final PartnerRepository partnerRepository;
    private final TransactionRepository transactionRepository;
    private final ProjectSummaryService projectSummaryService;

    public PartnerSettlementService(PartnerRepository partnerRepository,
                                    TransactionRepository transactionRepository,
                                    ProjectSummaryService projectSummaryService) {
        this.partnerRepository = partnerRepository;
        this.transactionRepository = transactionRepository;
        this.projectSummaryService = projectSummaryService;
    }

    public List<PartnerSettlementResponse> getSettlement(UUID projectId, UUID userId) {
        var summary = projectSummaryService.getSummary(projectId, userId);
        BigDecimal totalExpense = summary.getTotalExpense();
        BigDecimal profit = summary.getProfit();

        List<Partner> partners = partnerRepository.findByProjectId(projectId);
        return partners.stream()
                .map(p -> computeSettlement(p, projectId, totalExpense, profit))
                .collect(Collectors.toList());
    }

    private PartnerSettlementResponse computeSettlement(
            Partner partner, UUID projectId, BigDecimal totalExpense, BigDecimal profit) {

        BigDecimal shareFraction = partner.getSharePercentage()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal expectedContribution = totalExpense.multiply(shareFraction)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal actualPaid = orZero(transactionRepository.sumActiveByProjectTypeAndPaidBy(
                projectId, TransactionType.EXPENSE.name(), partner.getId()));

        BigDecimal settlementGap = actualPaid.subtract(expectedContribution);

        BigDecimal partnerProfitShare = profit.multiply(shareFraction)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal withdrawn = orZero(transactionRepository.sumActiveByProjectTypeAndPartner(
                projectId, TransactionType.PROFIT_WITHDRAWAL.name(), partner.getId()));

        BigDecimal netProfitDue = partnerProfitShare.subtract(withdrawn);

        return new PartnerSettlementResponse(
                partner.getId(),
                partner.getName(),
                partner.getSharePercentage(),
                expectedContribution,
                actualPaid,
                settlementGap,
                partnerProfitShare,
                withdrawn,
                netProfitDue
        );
    }

    private BigDecimal orZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
