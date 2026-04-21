package com.seedhahisaab.controller;

import com.seedhahisaab.dto.summary.VendorLedgerResponse;
import com.seedhahisaab.dto.vendor.VendorRequest;
import com.seedhahisaab.dto.vendor.VendorResponse;
import com.seedhahisaab.service.ProjectService;
import com.seedhahisaab.service.VendorLedgerService;
import com.seedhahisaab.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VendorLedgerService vendorLedgerService;
    private final ProjectService projectService;

    public VendorController(VendorService vendorService, VendorLedgerService vendorLedgerService,
                             ProjectService projectService) {
        this.vendorService = vendorService;
        this.vendorLedgerService = vendorLedgerService;
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorRequest req) {
        return ResponseEntity.ok(vendorService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAll() {
        return ResponseEntity.ok(vendorService.getAll());
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<VendorLedgerResponse> getLedger(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @AuthenticationPrincipal UserDetails user) {
        projectService.requireProject(projectId, UUID.fromString(user.getUsername()));
        return ResponseEntity.ok(vendorLedgerService.getLedger(id, projectId));
    }
}
