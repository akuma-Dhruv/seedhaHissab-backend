package com.seedhahisaab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A person/organisation the user expects to receive money from. Customers are
 * <strong>global per user</strong> — the same customer can be linked into
 * multiple projects via {@link ProjectCustomer}.
 *
 * <p>Customer rows hold zero financial truth: no balances, no totals, no
 * payment history. All of that lives on transactions and is derived on read.
 */
@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_cust_owner_name_lower",
                       columnList = "created_by_user_id, name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "notes", length = 1024)
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
