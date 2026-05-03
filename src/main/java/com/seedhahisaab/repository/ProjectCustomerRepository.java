package com.seedhahisaab.repository;

import com.seedhahisaab.entity.ProjectCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectCustomerRepository extends JpaRepository<ProjectCustomer, UUID> {

    Optional<ProjectCustomer> findByProjectIdAndCustomerId(UUID projectId, UUID customerId);

    List<ProjectCustomer> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndCustomerId(UUID projectId, UUID customerId);
}
