package com.example.rocketweb.service;

import com.example.rocketweb.core.OrkLoader;
import com.example.rocketweb.core.OrkWriter;
import com.example.rocketweb.core.RocketCalculator;
import com.example.rocketweb.model.ProjectDto;
import com.example.rocketweb.repository.ProjectRepository;

import net.sf.openrocket.document.OpenRocketDocument;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Business logic for project lifecycle management.
 * Coordinates between OrkLoader/OrkWriter, RocketCalculator, and the repository.
 */
@Service
public class ProjectService {

    private final OrkLoader orkLoader;
    private final OrkWriter orkWriter;
    private final RocketCalculator rocketCalculator;
    private final ProjectRepository projectRepository;

    public ProjectService(OrkLoader orkLoader,
                          OrkWriter orkWriter,
                          RocketCalculator rocketCalculator,
                          ProjectRepository projectRepository) {
        this.orkLoader = orkLoader;
        this.orkWriter = orkWriter;
        this.rocketCalculator = rocketCalculator;
        this.projectRepository = projectRepository;
    }

    /**
     * Imports a .ork file uploaded by the client.
     * Loads the document, calculates metrics, stores the project, and returns a DTO.
     *
     * @param file multipart .ork file
     * @return ProjectDto populated with metrics
     * @throws Exception if loading or calculation fails
     */
    public ProjectDto importFromOrkFile(MultipartFile file) throws Exception {
        File tmp = Files.createTempFile("ork-import-", ".ork").toFile();
        try {
            file.transferTo(tmp);
            OpenRocketDocument document = orkLoader.load(tmp);
            return createProjectFromDocument(document, stripExtension(file.getOriginalFilename()));
        } finally {
            tmp.delete();
        }
    }

    /**
     * Retrieves the ProjectDto for an existing project.
     *
     * @param id project ID
     * @return ProjectDto or null if not found
     */
    public ProjectDto getProject(String id) {
        return projectRepository.findDtoById(id);
    }

    /**
     * Writes the current state of the project to a temporary .ork file and returns it.
     *
     * @param id project ID
     * @return File containing the .ork data; caller is responsible for deleting it
     * @throws IOException if writing fails
     */
    public File exportToOrkFile(String id) throws IOException {
        OpenRocketDocument document = projectRepository.findDocumentById(id);
        if (document == null) {
            return null;
        }
        File tmp = Files.createTempFile("ork-export-", ".ork").toFile();
        orkWriter.write(document, tmp);
        return tmp;
    }

    // --- helpers ---

    private ProjectDto createProjectFromDocument(OpenRocketDocument document, String name) {
        String id = UUID.randomUUID().toString();

        ProjectDto dto = new ProjectDto();
        dto.setId(id);
        dto.setName(name != null ? name : "Untitled");

        rocketCalculator.calculate(document, dto);

        projectRepository.save(id, dto, document);
        return dto;
    }

    private String stripExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
