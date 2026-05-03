package com.seedhahisaab.service;

import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementRequest;
import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementResponse;
import com.seedhahisaab.dto.hidden.HiddenPartnerAgreementUpdateRequest;
import com.seedhahisaab.entity.HiddenPartnerAgreement;
import com.seedhahisaab.entity.Partner;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.HiddenPartnerAgreementRepository;
import com.seedhahisaab.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CRUD + privacy enforcement for {@link HiddenPartnerAgreement}.
 *
 * <p>This service owns three rules nothing else may bypass:
 * <ol>
 *   <li><strong>Project access:</strong> the caller must own the project
 *       (delegated to {@link ProjectService#requireProject}).</li>
 *   <li><strong>Creator ownership:</strong> every read and every mutation is
 *       scoped by {@code createdByUserId}, including reads of single rows.</li>
 *   <li><strong>100% cap:</strong> the sum of active hidden share percentages
 *       on any single official partner cannot exceed 100, and is checked on
 *       both create and update (with the row being updated excluded from the
 *       running sum so its own value isn't double-counted).</li>
 * </ol>
 *
 * <p>Single-level only: the {@code officialPartnerId} field on
 * {@link HiddenPartnerAgreement} references {@link Partner} (official) and
 * never another hidden agreement. There is no FK from hidden to hidden,
 * and no API surface that would let one be created.
 */
@Service
public class HiddenPartnerAgreementService {

    private final HiddenPartnerAgreementRepository repo;
    private final PartnerRepository partnerRepository;
    private final ProjectService projectService;

    public HiddenPartnerAgreementService(HiddenPartnerAgreementRepository repo,
                                         PartnerRepository partnerRepository,
                                         ProjectService projectService) {
        this.repo = repo;
        this.partnerRepository = partnerRepository;
        this.projectService = projectService;
    }

    // -- Reads ----------------------------------------------------------------

    public List<HiddenPartnerAgreementResponse> listForProject(UUID projectId,
                                                               UUID userId,
                                                               boolean includeArchived) {
        projectService.requireProject(projectId, userId);
        List<HiddenPartnerAgreement> rows = includeArchived
                ? repo.findAllForProject(projectId, userId)
                : repo.findActiveForProject(projectId, userId);
        Map<UUID, String> partnerNames = partnerNamesForProject(projectId);
        return rows.stream()
                .map(h -> HiddenPartnerAgreementResponse.from(
                        h, partnerNames.getOrDefault(h.getOfficialPartnerId(), "Unknown")))
                .collect(Collectors.toList());
    }

    public HiddenPartnerAgreementResponse get(UUID id, UUID userId) {
        HiddenPartnerAgreement h = requireOwned(id, userId);
        // Defence in depth: project access still required even though
        // the row is creator-scoped.
        projectService.requireProject(h.getProjectId(), userId);
        String name = partnerNamesForProject(h.getProjectId())
                .getOrDefault(h.getOfficialPartnerId(), "Unknown");
        return HiddenPartnerAgreementResponse.from(h, name);
    }

    public HiddenPartnerAgreement requireOwned(UUID id, UUID userId) {
        return repo.findByIdAndCreatedByUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Hidden agreement not found"));
    }

    /**
     * Used by {@link HiddenSettlementService} to fetch the active rows it
     * needs to derive private settlements. Going through this method keeps
     * the project + creator access checks in one place.
     */
    public List<HiddenPartnerAgreement> requireActiveForProject(UUID projectId, UUID userId) {
        projectService.requireProject(projectId, userId);
        return repo.findActiveForProject(projectId, userId);
    }

    // -- Mutations ------------------------------------------------------------

    @Transactional
    public HiddenPartnerAgreementResponse create(UUID projectId,
                                                 HiddenPartnerAgreementRequest req,
                                                 UUID userId) {
        projectService.requireProject(projectId, userId);
        Partner partner = requirePartnerInProject(req.getOfficialPartnerId(), projectId);

        validateCapOrThrow(partner, req.getSharePercentage(), null, userId);

        HiddenPartnerAgreement h = HiddenPartnerAgreement.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .officialPartnerId(partner.getId())
                .hiddenPartnerName(req.getHiddenPartnerName().trim())
                .hiddenPartnerUserId(null) // reserved for future use
                .sharePercentage(req.getSharePercentage())
                .notes(emptyToNull(req.getNotes()))
                .createdByUserId(userId)
                .build();
        h = repo.save(h);
        return HiddenPartnerAgreementResponse.from(h, partner.getName());
    }

    @Transactional
    public HiddenPartnerAgreementResponse update(UUID id,
                                                 HiddenPartnerAgreementUpdateRequest req,
                                                 UUID userId) {
        HiddenPartnerAgreement h = requireOwned(id, userId);
        projectService.requireProject(h.getProjectId(), userId);

        if (h.getArchivedAt() != null) {
            throw ApiException.conflict(
                    "This agreement is archived. Restore it before editing.");
        }

        if (req.getSharePercentage() != null) {
            Partner partner = requirePartnerInProject(h.getOfficialPartnerId(), h.getProjectId());
            validateCapOrThrow(partner, req.getSharePercentage(), h.getId(), userId);
            h.setSharePercentage(req.getSharePercentage());
        }
        if (req.getNotes() != null) {
            h.setNotes(emptyToNull(req.getNotes()));
        }
        h = repo.save(h);

        String name = partnerNamesForProject(h.getProjectId())
                .getOrDefault(h.getOfficialPartnerId(), "Unknown");
        return HiddenPartnerAgreementResponse.from(h, name);
    }

    @Transactional
    public HiddenPartnerAgreementResponse archive(UUID id, UUID userId) {
        HiddenPartnerAgreement h = requireOwned(id, userId);
        projectService.requireProject(h.getProjectId(), userId);
        if (h.getArchivedAt() == null) {
            h.setArchivedAt(Instant.now());
            h = repo.save(h);
        }
        String name = partnerNamesForProject(h.getProjectId())
                .getOrDefault(h.getOfficialPartnerId(), "Unknown");
        return HiddenPartnerAgreementResponse.from(h, name);
    }

    /** Used by future un-archive flow; not exposed publicly in v1. */
    @Transactional
    public HiddenPartnerAgreementResponse restore(UUID id, UUID userId) {
        HiddenPartnerAgreement h = requireOwned(id, userId);
        projectService.requireProject(h.getProjectId(), userId);
        if (h.getArchivedAt() != null) {
            // Re-validate the cap because other agreements may have grown
            // while this one was archived.
            Partner partner = requirePartnerInProject(h.getOfficialPartnerId(), h.getProjectId());
            validateCapOrThrow(partner, h.getSharePercentage(), h.getId(), userId);
            h.setArchivedAt(null);
            h = repo.save(h);
        }
        String name = partnerNamesForProject(h.getProjectId())
                .getOrDefault(h.getOfficialPartnerId(), "Unknown");
        return HiddenPartnerAgreementResponse.from(h, name);
    }

    // -- Helpers --------------------------------------------------------------

    private Partner requirePartnerInProject(UUID partnerId, UUID projectId) {
        Partner p = partnerRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("Official partner not found"));
        if (!p.getProjectId().equals(projectId)) {
            throw ApiException.badRequest(
                    "Selected partner does not belong to this project.");
        }
        return p;
    }

    private void validateCapOrThrow(Partner partner,
                                    BigDecimal newShare,
                                    UUID excludeAgreementId,
                                    UUID userId) {
        BigDecimal already = repo.sumActiveSharePctForOfficialPartner(
                partner.getId(), userId, excludeAgreementId);
        if (already == null) already = BigDecimal.ZERO;
        BigDecimal projected = already.add(newShare);
        if (projected.compareTo(BigDecimal.valueOf(100)) > 0) {
            BigDecimal remaining = BigDecimal.valueOf(100).subtract(already);
            if (remaining.signum() < 0) remaining = BigDecimal.ZERO;
            throw ApiException.conflict(String.format(
                    "Hidden shares on %s would exceed 100%%. " +
                    "Already promised: %s%%. Remaining: %s%%.",
                    partner.getName(),
                    already.stripTrailingZeros().toPlainString(),
                    remaining.stripTrailingZeros().toPlainString()));
        }
    }

    private Map<UUID, String> partnerNamesForProject(UUID projectId) {
        Map<UUID, String> map = new HashMap<>();
        for (Partner p : partnerRepository.findByProjectId(projectId)) {
            map.put(p.getId(), p.getName());
        }
        return map;
    }

    private static String emptyToNull(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
