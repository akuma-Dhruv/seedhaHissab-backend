package com.seedhahisaab.service;

import com.seedhahisaab.dto.customer.CustomerRequest;
import com.seedhahisaab.dto.customer.CustomerResponse;
import com.seedhahisaab.entity.Customer;
import com.seedhahisaab.entity.ProjectCustomer;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.CustomerRepository;
import com.seedhahisaab.repository.InstallmentRepository;
import com.seedhahisaab.repository.ProjectCustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages global-per-user customers and their project links. Customers carry
 * no balance — payment totals are computed via the installment + transaction
 * tables, never stored here.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProjectCustomerRepository projectCustomerRepository;
    private final InstallmentRepository installmentRepository;
    private final ProjectService projectService;

    public CustomerService(CustomerRepository customerRepository,
                           ProjectCustomerRepository projectCustomerRepository,
                           InstallmentRepository installmentRepository,
                           ProjectService projectService) {
        this.customerRepository = customerRepository;
        this.projectCustomerRepository = projectCustomerRepository;
        this.installmentRepository = installmentRepository;
        this.projectService = projectService;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req, UUID userId) {
        Customer c = Customer.builder()
                .id(UUID.randomUUID())
                .name(req.getName().trim())
                .phone(emptyToNull(req.getPhone()))
                .notes(emptyToNull(req.getNotes()))
                .createdByUserId(userId)
                .build();
        return CustomerResponse.from(customerRepository.save(c));
    }

    @Transactional
    public CustomerResponse update(UUID customerId, CustomerRequest req, UUID userId) {
        Customer c = requireOwned(customerId, userId);
        c.setName(req.getName().trim());
        c.setPhone(emptyToNull(req.getPhone()));
        c.setNotes(emptyToNull(req.getNotes()));
        return CustomerResponse.from(customerRepository.save(c));
    }

    public List<CustomerResponse> listForUser(UUID userId, String search) {
        String trimmed = search == null ? null : search.trim();
        if (trimmed != null && trimmed.isEmpty()) trimmed = null;
        return customerRepository.findAllForUser(userId, trimmed)
                .stream().map(CustomerResponse::from).collect(Collectors.toList());
    }

    public CustomerResponse get(UUID customerId, UUID userId) {
        return CustomerResponse.from(requireOwned(customerId, userId));
    }

    /**
     * Soft-block delete: refuses if any installments still reference the
     * customer. Cleaner than cascading away expected-receivable history.
     */
    @Transactional
    public void delete(UUID customerId, UUID userId) {
        Customer c = requireOwned(customerId, userId);
        // Targeted count — never load the entire installments table.
        if (installmentRepository.countByCustomerIdAndCreatedByUserId(customerId, userId) > 0) {
            throw ApiException.conflict(
                    "Cannot delete customer with linked installments. Cancel installments first.");
        }
        customerRepository.delete(c);
    }

    // -- Project-customer link methods ---------------------------------------

    @Transactional
    public CustomerResponse attachToProject(UUID projectId, UUID customerId, UUID userId) {
        projectService.requireProject(projectId, userId);
        Customer customer = requireOwned(customerId, userId);
        if (!projectCustomerRepository.existsByProjectIdAndCustomerId(projectId, customerId)) {
            ProjectCustomer link = ProjectCustomer.builder()
                    .id(UUID.randomUUID())
                    .projectId(projectId)
                    .customerId(customerId)
                    .build();
            projectCustomerRepository.save(link);
        }
        return CustomerResponse.from(customer);
    }

    public List<CustomerResponse> listForProject(UUID projectId, UUID userId) {
        projectService.requireProject(projectId, userId);
        List<UUID> customerIds = projectCustomerRepository.findByProjectId(projectId).stream()
                .map(ProjectCustomer::getCustomerId).toList();
        if (customerIds.isEmpty()) return List.of();
        return customerRepository.findAllById(customerIds).stream()
                .filter(c -> userId.equals(c.getCreatedByUserId()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void detachFromProject(UUID projectId, UUID customerId, UUID userId) {
        projectService.requireProject(projectId, userId);
        projectCustomerRepository.findByProjectIdAndCustomerId(projectId, customerId)
                .ifPresent(projectCustomerRepository::delete);
    }

    // -- Helpers --------------------------------------------------------------

    public Customer requireOwned(UUID customerId, UUID userId) {
        return customerRepository.findByIdAndCreatedByUserId(customerId, userId)
                .orElseThrow(() -> ApiException.notFound("Customer not found"));
    }

    private static String emptyToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
