package com.seedhahisaab.dto.customer;

import com.seedhahisaab.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {
    private UUID id;
    private String name;
    private String phone;
    private String notes;
    private UUID createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    public static CustomerResponse from(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .notes(c.getNotes())
                .createdByUserId(c.getCreatedByUserId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
