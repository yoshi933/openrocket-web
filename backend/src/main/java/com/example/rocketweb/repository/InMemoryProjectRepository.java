package com.example.rocketweb.repository;

import com.example.rocketweb.model.ProjectDto;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProjectRepository implements ProjectRepository {

    private final ConcurrentHashMap<String, ProjectDto> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ProjectDto project) {
        storage.put(project.getId(), project);
    }

    @Override
    public ProjectDto findById(String id) {
        return storage.get(id);
    }
}
