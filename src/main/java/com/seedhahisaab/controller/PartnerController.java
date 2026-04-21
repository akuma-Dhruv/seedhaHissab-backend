package com.seedhahisaab.controller;

import com.seedhahisaab.dto.partner.PartnerRequest;
import com.seedhahisaab.dto.partner.PartnerResponse;
import com.seedhahisaab.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/partners")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(partnerService.getById(id, currentUserId(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PartnerRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(partnerService.update(id, req, currentUserId(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        partnerService.delete(id, currentUserId(user));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
