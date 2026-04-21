package com.seedhahisaab.dto.partner;

import com.seedhahisaab.entity.Partner;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PartnerResponse {
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private String name;
    private BigDecimal sharePercentage;

    public static PartnerResponse from(Partner p) {
        PartnerResponse r = new PartnerResponse();
        r.setId(p.getId());
        r.setProjectId(p.getProjectId());
        r.setUserId(p.getUserId());
        r.setName(p.getName());
        r.setSharePercentage(p.getSharePercentage());
        return r;
    }
}
