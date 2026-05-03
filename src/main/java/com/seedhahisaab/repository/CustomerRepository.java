package com.seedhahisaab.repository;

import com.seedhahisaab.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndCreatedByUserId(UUID id, UUID userId);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.createdByUserId = :userId
              AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY LOWER(c.name) ASC
            """)
    List<Customer> findAllForUser(
            @Param("userId") UUID userId,
            @Param("search") String search);
}
