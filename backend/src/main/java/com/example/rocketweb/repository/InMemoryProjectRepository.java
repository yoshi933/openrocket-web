package com.example.rocketweb.repository;

import com.example.rocketweb.model.ProjectDto;
import net.sf.openrocket.document.OpenRocketDocument;

import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of ProjectRepository.
 * Stores both the lightweight DTO and the full OpenRocketDocument
 * so they stay in sync.
 */
@Repository
public class InMemoryProjectRepository implements ProjectRepository {

    private final ConcurrentHashMap<String, ProjectDto> dtos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OpenRocketDocument> documents = new ConcurrentHashMap<>();

    @Override
    public void save(String id, ProjectDto dto, OpenRocketDocument document) {
        dtos.put(id, dto);
        documents.put(id, document);
    }

    @Override
    public ProjectDto findDtoById(String id) {
        return dtos.get(id);
    }

    @Override
    public OpenRocketDocument findDocumentById(String id) {
        return documents.get(id);
    }

    @Override
    public boolean delete(String id) {
        boolean existed = dtos.containsKey(id);
        dtos.remove(id);
        documents.remove(id);
        return existed;
    }
}
