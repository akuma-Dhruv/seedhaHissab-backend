package com.seedhahisaab.dto.vendor;

import com.seedhahisaab.entity.Vendor;
import lombok.Data;

import java.util.UUID;

@Data
public class VendorResponse {
    private UUID id;
    private String name;
    private String contactInfo;

    public static VendorResponse from(Vendor v) {
        VendorResponse r = new VendorResponse();
        r.setId(v.getId());
        r.setName(v.getName());
        r.setContactInfo(v.getContactInfo());
        return r;
    }
}
