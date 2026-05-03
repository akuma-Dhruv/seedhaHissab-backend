package com.seedhahisaab.dto.member;

import com.seedhahisaab.domain.ProjectMemberRole;
import com.seedhahisaab.entity.ProjectMember;
import com.seedhahisaab.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Read DTO for a project membership row. Includes the linked user's
 * display name and email so the UI doesn't have to chase a second lookup.
 */
@Data
@Builder
public class ProjectMemberResponse {
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private ProjectMemberRole role;
    private UUID invitedByUserId;
    private Instant createdAt;
    private Instant archivedAt;

    public static ProjectMemberResponse from(ProjectMember m, User user) {
        return ProjectMemberResponse.builder()
                .id(m.getId())
                .projectId(m.getProjectId())
                .userId(m.getUserId())
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .role(m.getRole())
                .invitedByUserId(m.getInvitedByUserId())
                .createdAt(m.getCreatedAt())
                .archivedAt(m.getArchivedAt())
                .build();
    }
}
