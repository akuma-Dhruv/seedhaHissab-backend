package com.seedhahisaab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure link table between a project and a (global-per-user) customer.
 * Existence of a row only means "this customer is associated with this
 * project". The same customer can appear in many projects.
 *
 * <p>Stores no balances and no settlement state.
 */
@Entity
@Table(name = "project_customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_customer",
                                  columnNames = {"project_id", "customer_id"})
        },
        indexes = {
                @Index(name = "idx_pc_project", columnList = "project_id"),
                @Index(name = "idx_pc_customer", columnList = "customer_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCustomer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
