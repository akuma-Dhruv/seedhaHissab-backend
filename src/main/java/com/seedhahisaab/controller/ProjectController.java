package com.seedhahisaab.controller;

import com.seedhahisaab.dto.partner.PartnerRequest;
import com.seedhahisaab.dto.partner.PartnerResponse;
import com.seedhahisaab.dto.project.ProjectRequest;
import com.seedhahisaab.dto.project.ProjectResponse;
import com.seedhahisaab.dto.summary.PartnerSettlementResponse;
import com.seedhahisaab.dto.summary.ProjectSummaryResponse;
import com.seedhahisaab.dto.summary.VendorLedgerResponse;
import com.seedhahisaab.dto.vendor.VendorResponse;
import com.seedhahisaab.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final PartnerService partnerService;
    private final VendorService vendorService;
    private final ProjectSummaryService projectSummaryService;
    private final VendorLedgerService vendorLedgerService;
    private final PartnerSettlementService partnerSettlementService;

    public ProjectController(ProjectService projectService,
                             PartnerService partnerService,
                             VendorService vendorService,
                             ProjectSummaryService projectSummaryService,
                             VendorLedgerService vendorLedgerService,
                             PartnerSettlementService partnerSettlementService) {
        this.projectService = projectService;
        this.partnerService = partnerService;
        this.vendorService = vendorService;
        this.projectSummaryService = projectSummaryService;
        this.vendorLedgerService = vendorLedgerService;
        this.partnerSettlementService = partnerSettlementService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(projectService.create(req, currentUserId(user)));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(projectService.getAllForUser(currentUserId(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(projectService.getById(id, currentUserId(user)));
    }

    @PostMapping("/{id}/partners")
    public ResponseEntity<PartnerResponse> addPartner(
            @PathVariable UUID id,
            @Valid @RequestBody PartnerRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(partnerService.add(id, req, currentUserId(user)));
    }

    @GetMapping("/{id}/partners")
    public ResponseEntity<List<PartnerResponse>> getPartners(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(partnerService.getByProject(id, currentUserId(user)));
    }

    @GetMapping("/{id}/vendors")
    public ResponseEntity<List<VendorResponse>> getVendors(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        projectService.requireProject(id, currentUserId(user));
        return ResponseEntity.ok(vendorService.getByProject(id));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ProjectSummaryResponse> getSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(projectSummaryService.getSummary(id, currentUserId(user)));
    }

    @GetMapping("/{id}/vendors/ledger")
    public ResponseEntity<List<VendorLedgerResponse>> getVendorLedgers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        projectService.requireProject(id, currentUserId(user));
        return ResponseEntity.ok(vendorLedgerService.getAllLedgers(id));
    }

    @GetMapping("/{id}/settlement")
    public ResponseEntity<List<PartnerSettlementResponse>> getSettlement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(partnerSettlementService.getSettlement(id, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
