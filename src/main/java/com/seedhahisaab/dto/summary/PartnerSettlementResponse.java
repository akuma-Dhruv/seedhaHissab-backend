package com.seedhahisaab.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PartnerSettlementResponse {
    private UUID partnerId;
    private String partnerName;
    private BigDecimal sharePercentage;
    private BigDecimal expectedContribution;
    private BigDecimal actualPaid;
    private BigDecimal settlementGap;
    private BigDecimal partnerProfitShare;
    private BigDecimal withdrawn;
    private BigDecimal netProfitDue;
}
