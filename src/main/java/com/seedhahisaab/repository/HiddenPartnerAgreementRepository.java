package com.seedhahisaab.repository;

import com.seedhahisaab.entity.HiddenPartnerAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link HiddenPartnerAgreement}. All non-trivial queries are
 * scoped by {@code createdByUserId} so the privacy boundary is enforced at
 * the data-access layer, not just the service layer.
 */
@Repository
public interface HiddenPartnerAgreementRepository
        extends JpaRepository<HiddenPartnerAgreement, UUID> {

    Optional<HiddenPartnerAgreement> findByIdAndCreatedByUserId(UUID id, UUID userId);

    /** Active agreements (not archived) created by this user on this project. */
    @Query("""
            SELECT h FROM HiddenPartnerAgreement h
            WHERE h.projectId = :projectId
              AND h.createdByUserId = :userId
              AND h.archivedAt IS NULL
            ORDER BY h.officialPartnerId, h.createdAt
            """)
    List<HiddenPartnerAgreement> findActiveForProject(
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId);

    /** Active + archived agreements created by this user on this project. */
    @Query("""
            SELECT h FROM HiddenPartnerAgreement h
            WHERE h.projectId = :projectId
              AND h.createdByUserId = :userId
            ORDER BY h.archivedAt NULLS FIRST, h.officialPartnerId, h.createdAt
            """)
    List<HiddenPartnerAgreement> findAllForProject(
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId);

    /**
     * Sum of active hidden share percentages already promised on a given
     * official partner, optionally excluding one agreement (used when
     * validating an update so the row's own share isn't double-counted).
     */
    @Query("""
            SELECT COALESCE(SUM(h.sharePercentage), 0)
            FROM HiddenPartnerAgreement h
            WHERE h.officialPartnerId = :officialPartnerId
              AND h.createdByUserId = :userId
              AND h.archivedAt IS NULL
              AND (:excludeId IS NULL OR h.id <> :excludeId)
            """)
    BigDecimal sumActiveSharePctForOfficialPartner(
            @Param("officialPartnerId") UUID officialPartnerId,
            @Param("userId") UUID userId,
            @Param("excludeId") UUID excludeId);

    long countByOfficialPartnerIdAndCreatedByUserId(UUID officialPartnerId, UUID userId);
}
