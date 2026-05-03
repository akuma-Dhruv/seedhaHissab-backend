package com.seedhahisaab.controller;

import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.summary.CounterpartySummaryResponse;
import com.seedhahisaab.dto.summary.PersonalSummaryResponse;
import com.seedhahisaab.dto.transaction.PersonalTransactionRequest;
import com.seedhahisaab.dto.transaction.TransactionResponse;
import com.seedhahisaab.service.PersonalTransactionService;
import com.seedhahisaab.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for PERSONAL (person-to-person) transactions.
 *
 * All endpoints are scoped to the JWT-authenticated user via {@code ownerUserId};
 * a user can never read or modify another user's personal transactions.
 *
 * History is delegated to {@link TransactionService#getHistory} which already
 * supports both project and personal transactions via its ownership check.
 */
@RestController
@RequestMapping("/personal")
public class PersonalTransactionController {

    private final PersonalTransactionService personalTransactionService;
    private final TransactionService transactionService;

    public PersonalTransactionController(PersonalTransactionService personalTransactionService,
                                         TransactionService transactionService) {
        this.personalTransactionService = personalTransactionService;
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody PersonalTransactionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.create(req, currentUserId(user)));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PersonalTransactionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.update(id, req, currentUserId(user)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PagedResponse<TransactionResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeOmitted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.list(
                currentUserId(user), includeOmitted, page, limit));
    }

    @GetMapping("/summary")
    public ResponseEntity<PersonalSummaryResponse> summary(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.summary(currentUserId(user)));
    }

    @GetMapping("/transactions/{id}/history")
    public ResponseEntity<List<TransactionResponse>> history(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(transactionService.getHistory(id, currentUserId(user)));
    }

    @GetMapping("/counterparties")
    public ResponseEntity<PagedResponse<CounterpartySummaryResponse>> counterparties(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.counterparties(
                currentUserId(user), search, page, limit));
    }

    @GetMapping("/counterparties/{name}/ledger")
    public ResponseEntity<PagedResponse<TransactionResponse>> counterpartyLedger(
            @PathVariable("name") String name,
            @RequestParam(defaultValue = "false") boolean includeOmitted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(personalTransactionService.counterpartyLedger(
                currentUserId(user), name, includeOmitted, page, limit));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
