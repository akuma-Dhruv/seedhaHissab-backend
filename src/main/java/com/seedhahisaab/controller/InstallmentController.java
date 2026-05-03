package com.seedhahisaab.controller;

import com.seedhahisaab.domain.InstallmentDerivedStatus;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.installment.InstallmentRequest;
import com.seedhahisaab.dto.installment.InstallmentResponse;
import com.seedhahisaab.dto.installment.InstallmentSummaryResponse;
import com.seedhahisaab.service.InstallmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for installments. Project-scoped routes for create/list/
 * summary; flat routes for single-installment fetch and update/cancel.
 *
 * <p>The status / receivedAmount / remainingAmount fields in every response
 * are derived authoritatively by {@link InstallmentService}; clients render
 * those values as-is.
 */
@RestController
public class InstallmentController {

    private final InstallmentService installmentService;

    public InstallmentController(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    @PostMapping("/projects/{projectId}/installments")
    public ResponseEntity<InstallmentResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody InstallmentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.create(projectId, req, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/installments")
    public ResponseEntity<PagedResponse<InstallmentResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) InstallmentDerivedStatus status,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.listByProject(
                projectId, currentUserId(user), status, overdueOnly, customerId, page, limit));
    }

    @GetMapping("/projects/{projectId}/installments/summary")
    public ResponseEntity<InstallmentSummaryResponse> summary(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.summaryForProject(projectId, currentUserId(user)));
    }

    @GetMapping("/installments/{id}")
    public ResponseEntity<InstallmentResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.get(id, currentUserId(user)));
    }

    @PutMapping("/installments/{id}")
    public ResponseEntity<InstallmentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody InstallmentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.update(id, req, currentUserId(user)));
    }

    @PatchMapping("/installments/{id}/cancel")
    public ResponseEntity<InstallmentResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.cancel(id, currentUserId(user)));
    }

    @PatchMapping("/installments/{id}/reopen")
    public ResponseEntity<InstallmentResponse> reopen(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(installmentService.reopen(id, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
