package com.seedhahisaab.controller;

import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.transaction.TransactionRequest;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.create(req, currentUserId(user)));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.update(id, req, currentUserId(user)));
    }

    @PatchMapping("/transactions/{id}/omit")
    public ResponseEntity<TransactionResponse> omit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.omit(id, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/transactions")
    public ResponseEntity<PagedResponse<TransactionResponse>> getByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean includeOmitted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.getByProject(projectId, currentUserId(user),
                includeOmitted, page, limit));
    }

    @GetMapping("/transactions/{id}/latest")
    public ResponseEntity<TransactionResponse> getLatest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.getLatest(id, currentUserId(user)));
    }

    @GetMapping("/transactions/{id}/history")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.getHistory(id, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
