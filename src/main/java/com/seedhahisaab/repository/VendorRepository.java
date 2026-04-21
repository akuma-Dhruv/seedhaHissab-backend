package com.seedhahisaab.repository;

import com.seedhahisaab.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    @Query(value = """
            SELECT DISTINCT v.* FROM vendors v
            INNER JOIN (
                SELECT DISTINCT t.vendor_id FROM transactions t
                INNER JOIN (
                    SELECT root_transaction_id, MAX(version) AS max_version
                    FROM transactions
                    WHERE project_id = :projectId
                    GROUP BY root_transaction_id
                ) latest ON t.root_transaction_id = latest.root_transaction_id
                        AND t.version = latest.max_version
                WHERE t.status = 'ACTIVE'
                  AND t.vendor_id IS NOT NULL
            ) active_vendors ON v.id = active_vendors.vendor_id
            """, nativeQuery = true)
    List<Vendor> findByProjectId(@Param("projectId") UUID projectId);
}
