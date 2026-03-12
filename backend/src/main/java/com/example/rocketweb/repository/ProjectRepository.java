package com.example.rocketweb.repository;

import com.example.rocketweb.model.ProjectDto;

public interface ProjectRepository {

    void save(ProjectDto project);

    ProjectDto findById(String id);
}
