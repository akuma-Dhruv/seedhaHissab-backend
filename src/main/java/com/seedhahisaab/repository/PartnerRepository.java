package com.seedhahisaab.repository;

import com.seedhahisaab.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {
    List<Partner> findByProjectId(UUID projectId);
    boolean existsByProjectId(UUID projectId);
}
