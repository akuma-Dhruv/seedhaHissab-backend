package com.seedhahisaab.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String name;

    private String contactInfo;
}
