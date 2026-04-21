package com.seedhahisaab.service;

import com.seedhahisaab.dto.project.ProjectRequest;
import com.seedhahisaab.dto.project.ProjectResponse;
import com.seedhahisaab.entity.Project;
import com.seedhahisaab.exception.ApiException;
import com.seedhahisaab.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse create(ProjectRequest req, UUID userId) {
        Project project = Project.builder()
                .id(UUID.randomUUID())
                .name(req.getName())
                .plannedBudget(req.getPlannedBudget())
                .expectedRevenue(req.getExpectedRevenue())
                .createdBy(userId)
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    public List<ProjectResponse> getAllForUser(UUID userId) {
        return projectRepository.findByCreatedByOrderByCreatedAtDesc(userId)
                .stream().map(ProjectResponse::from).collect(Collectors.toList());
    }

    public ProjectResponse getById(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        if (!project.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return ProjectResponse.from(project);
    }

    public Project requireProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        if (!project.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }
        return project;
    }
}
