package com.seedhahisaab.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be 255 characters or fewer")
    private String name;

    @Size(max = 64, message = "Phone must be 64 characters or fewer")
    private String phone;

    @Size(max = 1024, message = "Notes must be 1024 characters or fewer")
    private String notes;
}
