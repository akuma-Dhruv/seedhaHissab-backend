package com.seedhahisaab.controller;

import com.seedhahisaab.domain.ReminderStatus;
import com.seedhahisaab.dto.common.PagedResponse;
import com.seedhahisaab.dto.reminder.ReminderRequest;
import com.seedhahisaab.dto.reminder.ReminderResponse;
import com.seedhahisaab.dto.reminder.ReminderSnoozeRequest;
import com.seedhahisaab.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the Reminder & Due Tracking engine.
 *
 * <p>Every endpoint is JWT-scoped — the authenticated user can only see
 * and modify their own reminders. See {@link ReminderService} for the
 * authoritative design rules (reminders are metadata, not financial truth).
 */
@RestController
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(
            @Valid @RequestBody ReminderRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.create(req, currentUserId(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReminderRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.update(id, req, currentUserId(user)));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ReminderResponse>> list(
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) LocalDate dueAfter,
            @RequestParam(required = false) LocalDate dueBefore,
            @RequestParam(required = false) UUID linkedProjectId,
            @RequestParam(required = false) String linkedCounterpartyName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.list(
                currentUserId(user),
                status,
                includeArchived,
                dueAfter,
                dueBefore,
                linkedProjectId,
                linkedCounterpartyName,
                page,
                limit));
    }

    @GetMapping("/today")
    public ResponseEntity<List<ReminderResponse>> today(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.today(currentUserId(user)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<ReminderResponse>> overdue(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.overdue(currentUserId(user)));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<ReminderResponse>> upcoming(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.upcoming(currentUserId(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.get(id, currentUserId(user)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ReminderResponse> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.complete(id, currentUserId(user)));
    }

    @PatchMapping("/{id}/snooze")
    public ResponseEntity<ReminderResponse> snooze(
            @PathVariable UUID id,
            @Valid @RequestBody ReminderSnoozeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.snooze(id, req, currentUserId(user)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ReminderResponse> archive(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reminderService.archive(id, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
