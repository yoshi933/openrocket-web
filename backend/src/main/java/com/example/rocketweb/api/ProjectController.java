package com.example.rocketweb.api;

import com.example.rocketweb.model.ProjectDto;
import com.example.rocketweb.service.ProjectService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * REST controller for project management.
 * All endpoints are prefixed with /api/projects.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * GET /api/projects/{id}
     * Returns the ProjectDto (with CG, CP, mass, stability) for the given project.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable String id) {
        ProjectDto dto = projectService.getProject(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/projects/import
     * Accepts a multipart .ork file, loads it via OrkLoader, calculates metrics,
     * and returns the resulting ProjectDto.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectDto> importOrkFile(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ProjectDto dto = projectService.importFromOrkFile(file);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to import .ork file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    /**
     * GET /api/projects/{id}/export
     * Exports the current project state to a .ork file and returns it as a download.
     * The temporary file is read into memory and deleted before the response is sent,
     * preventing resource leaks.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<ByteArrayResource> exportOrkFile(@PathVariable String id) {
        File tmp = null;
        try {
            tmp = projectService.exportToOrkFile(id);
            if (tmp == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] data = Files.readAllBytes(tmp.toPath());
            ByteArrayResource resource = new ByteArrayResource(data);

            String filename = id + ".ork";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to export project '{}': {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }
}
