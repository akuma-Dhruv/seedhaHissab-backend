package com.seedhahisaab.controller;

import com.seedhahisaab.dto.customer.CustomerRequest;
import com.seedhahisaab.dto.customer.CustomerResponse;
import com.seedhahisaab.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for customers. Customers are global per user — they're not
 * scoped to a project. Project linking is handled via
 * {@code POST /projects/{id}/customers/{customerId}} below.
 */
@RestController
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.create(req, currentUserId(user)));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.update(id, req, currentUserId(user)));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.listForUser(currentUserId(user), search));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.get(id, currentUserId(user)));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        customerService.delete(id, currentUserId(user));
        return ResponseEntity.noContent().build();
    }

    // -- Project link -----------------------------------------------------

    @GetMapping("/projects/{projectId}/customers")
    public ResponseEntity<List<CustomerResponse>> listForProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.listForProject(projectId, currentUserId(user)));
    }

    @PostMapping("/projects/{projectId}/customers/{customerId}")
    public ResponseEntity<CustomerResponse> attach(
            @PathVariable UUID projectId,
            @PathVariable UUID customerId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(customerService.attachToProject(projectId, customerId, currentUserId(user)));
    }

    @DeleteMapping("/projects/{projectId}/customers/{customerId}")
    public ResponseEntity<Void> detach(
            @PathVariable UUID projectId,
            @PathVariable UUID customerId,
            @AuthenticationPrincipal UserDetails user) {
        customerService.detachFromProject(projectId, customerId, currentUserId(user));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
