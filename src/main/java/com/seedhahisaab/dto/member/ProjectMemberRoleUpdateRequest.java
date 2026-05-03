package com.seedhahisaab.dto.member;

import com.seedhahisaab.domain.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectMemberRoleUpdateRequest {

    @NotNull(message = "Role is required")
    private ProjectMemberRole role;
}
