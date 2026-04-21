package com.seedhahisaab.dto.project;

import com.seedhahisaab.entity.Project;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class ProjectResponse {
    private UUID id;
    private String name;
    private BigDecimal plannedBudget;
    private BigDecimal expectedRevenue;
    private UUID createdBy;
    private Instant createdAt;

    public static ProjectResponse from(Project p) {
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setPlannedBudget(p.getPlannedBudget());
        r.setExpectedRevenue(p.getExpectedRevenue());
        r.setCreatedBy(p.getCreatedBy());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
