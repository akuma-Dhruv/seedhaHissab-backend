package com.seedhahisaab.controller;

import com.seedhahisaab.dto.member.ProjectMemberInviteRequest;
import com.seedhahisaab.dto.member.ProjectMemberResponse;
import com.seedhahisaab.dto.member.ProjectMemberRoleUpdateRequest;
import com.seedhahisaab.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> list(
            @PathVariable UUID projectId,
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(memberService.list(projectId, currentUserId(user), includeArchived));
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> invite(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectMemberInviteRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(memberService.invite(projectId, req, currentUserId(user)));
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<ProjectMemberResponse> updateRole(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Valid @RequestBody ProjectMemberRoleUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(memberService.updateRole(projectId, memberId, req, currentUserId(user)));
    }

    @PatchMapping("/{memberId}/archive")
    public ResponseEntity<ProjectMemberResponse> archive(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(memberService.archive(projectId, memberId, currentUserId(user)));
    }

    @PatchMapping("/{memberId}/restore")
    public ResponseEntity<ProjectMemberResponse> restore(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(memberService.restore(projectId, memberId, currentUserId(user)));
    }

    private UUID currentUserId(UserDetails user) {
        return UUID.fromString(user.getUsername());
    }
}
