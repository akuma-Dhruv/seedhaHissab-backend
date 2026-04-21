package com.seedhahisaab.service;

import com.seedhahisaab.dto.partner.PartnerRequest;
import com.seedhahisaab.dto.partner.PartnerResponse;
import com.seedhahisaab.entity.Partner;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.PartnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final ProjectService projectService;

    public PartnerService(PartnerRepository partnerRepository, ProjectService projectService) {
        this.partnerRepository = partnerRepository;
        this.projectService = projectService;
    }

    public PartnerResponse add(UUID projectId, PartnerRequest req, UUID userId) {
        projectService.requireProject(projectId, userId);
        Partner partner = Partner.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name(req.getName())
                .sharePercentage(req.getSharePercentage())
                .userId(req.getUserId())
                .build();
        return PartnerResponse.from(partnerRepository.save(partner));
    }

    public List<PartnerResponse> getByProject(UUID projectId, UUID userId) {
        projectService.requireProject(projectId, userId);
        return partnerRepository.findByProjectId(projectId)
                .stream().map(PartnerResponse::from).collect(Collectors.toList());
    }

    public PartnerResponse getById(UUID partnerId, UUID userId) {
        Partner partner = requireOwnedPartner(partnerId, userId);
        return PartnerResponse.from(partner);
    }

    public PartnerResponse update(UUID partnerId, PartnerRequest req, UUID userId) {
        Partner partner = requireOwnedPartner(partnerId, userId);
        if (req.getName() != null) {
            partner.setName(req.getName());
        }
        if (req.getSharePercentage() != null) {
            partner.setSharePercentage(req.getSharePercentage());
        }
        if (req.getUserId() != null) {
            partner.setUserId(req.getUserId());
        }
        return PartnerResponse.from(partnerRepository.save(partner));
    }

    public void delete(UUID partnerId, UUID userId) {
        requireOwnedPartner(partnerId, userId);
        partnerRepository.deleteById(partnerId);
    }

    public Partner requirePartner(UUID partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("Partner not found: " + partnerId));
    }

    private Partner requireOwnedPartner(UUID partnerId, UUID userId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("Partner not found"));
        projectService.requireProject(partner.getProjectId(), userId);
        return partner;
    }
}
