package com.seedhahisaab.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class VendorLedgerResponse {
    private UUID vendorId;
    private String vendorName;
    private UUID projectId;
    private BigDecimal totalSupply;
    private BigDecimal totalPaid;
    private BigDecimal balance;
}
