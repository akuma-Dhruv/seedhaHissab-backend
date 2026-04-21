package com.seedhahisaab.service;

import com.seedhahisaab.dto.vendor.VendorRequest;
import com.seedhahisaab.dto.vendor.VendorResponse;
import com.seedhahisaab.entity.Vendor;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public VendorResponse create(VendorRequest req) {
        Vendor vendor = Vendor.builder()
                .id(UUID.randomUUID())
                .name(req.getName())
                .contactInfo(req.getContactInfo())
                .build();
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    public List<VendorResponse> getAll() {
        return vendorRepository.findAll()
                .stream().map(VendorResponse::from).collect(Collectors.toList());
    }

    public List<VendorResponse> getByProject(UUID projectId) {
        return vendorRepository.findByProjectId(projectId)
                .stream().map(VendorResponse::from).collect(Collectors.toList());
    }

    public Vendor requireVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> ApiException.notFound("Vendor not found: " + vendorId));
    }
}
