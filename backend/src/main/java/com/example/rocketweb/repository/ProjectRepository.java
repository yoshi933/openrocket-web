package com.example.rocketweb.repository;

import com.example.rocketweb.model.ProjectDto;
import net.sf.openrocket.document.OpenRocketDocument;

/**
 * Contract for storing and retrieving active rocket projects.
 */
public interface ProjectRepository {

    /** Persist or update a project entry. */
    void save(String id, ProjectDto dto, OpenRocketDocument document);

    /** Return the DTO for the given project ID, or null if not found. */
    ProjectDto findDtoById(String id);

    /** Return the OpenRocketDocument for the given project ID, or null if not found. */
    OpenRocketDocument findDocumentById(String id);

    /** Remove a project. Returns true if it existed. */
    boolean delete(String id);
}
