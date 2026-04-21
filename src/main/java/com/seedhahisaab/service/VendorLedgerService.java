package com.seedhahisaab.service;

import com.seedhahisaab.domain.TransactionType;
import com.seedhahisaab.dto.summary.VendorLedgerResponse;
import com.seedhahisaab.entity.Vendor;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.TransactionRepository;
import com.seedhahisaab.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VendorLedgerService {

    private final TransactionRepository transactionRepository;
    private final VendorRepository vendorRepository;

    public VendorLedgerService(TransactionRepository transactionRepository, VendorRepository vendorRepository) {
        this.transactionRepository = transactionRepository;
        this.vendorRepository = vendorRepository;
    }

    public VendorLedgerResponse getLedger(UUID vendorId, UUID projectId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> ApiException.notFound("Vendor not found: " + vendorId));
        return buildLedger(vendor, projectId);
    }

    public List<VendorLedgerResponse> getAllLedgers(UUID projectId) {
        return vendorRepository.findByProjectId(projectId)
                .stream()
                .map(v -> buildLedger(v, projectId))
                .collect(Collectors.toList());
    }

    private VendorLedgerResponse buildLedger(Vendor vendor, UUID projectId) {
        BigDecimal totalSupply = orZero(transactionRepository.sumActiveByProjectVendorAndType(
                projectId, vendor.getId(), TransactionType.VENDOR_SUPPLY.name()));
        BigDecimal totalPaid = orZero(transactionRepository.sumActiveByProjectVendorAndType(
                projectId, vendor.getId(), TransactionType.VENDOR_PAYMENT.name()));
        BigDecimal balance = totalSupply.subtract(totalPaid);
        return new VendorLedgerResponse(vendor.getId(), vendor.getName(), projectId, totalSupply, totalPaid, balance);
    }

    private BigDecimal orZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
