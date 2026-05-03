package com.seedhahisaab.controller;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import com.seedhahisaab.dto.activity.ActivityFeedResponse;
import com.seedhahisaab.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only timeline endpoints.
 *
 * <p>Every endpoint:
 * <ul>
 *   <li>requires authentication (resolved via {@code SecurityConfig}),</li>
 *   <li>delegates ownership/visibility checks to {@link ActivityService}, and</li>
 *   <li>returns a flat ordered stream — date grouping is a frontend concern.</li>
 * </ul>
 *
 * <p>{@code visibilityScope} default behaviour: if absent, return both
 * OFFICIAL and the caller's own PRIVATE rows merged chronologically. The FE
 * filter (ALL/OFFICIAL/PRIVATE) is presentation only; the backend is the
 * authoritative gate.
 */
@RestController
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/projects/{projectId}/activity")
    public ResponseEntity<ActivityFeedResponse> forProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) FinancialVisibilityScope visibilityScope,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(activityService.forProject(
                projectId, currentUserId(user), page, limit, type, visibilityScope));
    }

    @GetMapping("/personal/activity")
    public ResponseEntity<ActivityFeedResponse> forPersonal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) FinancialVisibilityScope visibilityScope,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(activityService.forPersonal(
                currentUserId(user), page, limit, type, visibilityScope));
    }

    @GetMapping("/personal/counterparties/{name}/activity")
    public ResponseEntity<ActivityFeedResponse> forCounterparty(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) FinancialVisibilityScope visibilityScope,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(activityService.forCounterparty(
                currentUserId(user), name, page, limit, type, visibilityScope));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
