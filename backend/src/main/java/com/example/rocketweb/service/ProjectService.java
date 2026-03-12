package com.example.rocketweb.service;

import com.example.rocketweb.model.ProjectDto;
import com.example.rocketweb.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectDto createProject(ProjectDto dto) {
        ProjectDto project = new ProjectDto();
        project.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString());
        project.setName(dto.getName());
        projectRepository.save(project);
        return project;
    }

    public ProjectDto getProject(String id) {
        return projectRepository.findById(id);
    }
}
