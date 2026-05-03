package com.seedhahisaab.controller;

import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementRequest;
import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementResponse;
import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementUpdateRequest;
import com.seedhahisaab.dto.hidden.HiddenSettlementResponse;
import com.seedhahisaab.service.HiddenPartnerAgreementService;
import com.seedhahisaab.service.HiddenSettlementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the private hidden-partner overlay. Every route is
 * scoped to the authenticated user — see
 * {@link HiddenPartnerAgreementService} for ownership enforcement.
 *
 * <p><strong>Privacy boundary:</strong> none of these endpoints leak into
 * existing official routes ({@code /projects/.../partners},
 * {@code /projects/.../settlement}, {@code /projects/.../summary}). They are
 * an overlay, not a replacement.
 */
@RestController
public class HiddenPartnerController {

    private final HiddenPartnerAgreementService agreementService;
    private final HiddenSettlementService settlementService;

    public HiddenPartnerController(HiddenPartnerAgreementService agreementService,
                                   HiddenSettlementService settlementService) {
        this.agreementService = agreementService;
        this.settlementService = settlementService;
    }

    @PostMapping("/projects/{projectId}/hidden-partners")
    public ResponseEntity<HiddenPartnerAgreementResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody HiddenPartnerAgreementRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                agreementService.create(projectId, req, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/hidden-partners")
    public ResponseEntity<List<HiddenPartnerAgreementResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                agreementService.listForProject(projectId, currentUserId(user), includeArchived));
    }

    @GetMapping("/hidden-partners/{id}")
    public ResponseEntity<HiddenPartnerAgreementResponse> getOne(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(agreementService.get(id, currentUserId(user)));
    }

    @PatchMapping("/hidden-partners/{id}")
    public ResponseEntity<HiddenPartnerAgreementResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody HiddenPartnerAgreementUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(agreementService.update(id, req, currentUserId(user)));
    }

    @PatchMapping("/hidden-partners/{id}/archive")
    public ResponseEntity<HiddenPartnerAgreementResponse> archive(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(agreementService.archive(id, currentUserId(user)));
    }

    @PatchMapping("/hidden-partners/{id}/restore")
    public ResponseEntity<HiddenPartnerAgreementResponse> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(agreementService.restore(id, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/hidden-settlements")
    public ResponseEntity<HiddenSettlementResponse> settlements(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                settlementService.getForProject(projectId, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
