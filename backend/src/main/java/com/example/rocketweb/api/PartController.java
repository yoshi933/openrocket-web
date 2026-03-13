package com.example.rocketweb.api;

import com.example.rocketweb.model.PartDto;
import com.example.rocketweb.service.PartService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST controller for rocket part (component) management.
 * All endpoints are nested under /api/projects/{projectId}/parts.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/parts")
public class PartController {

    private static final Logger log = LoggerFactory.getLogger(PartController.class);

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    /**
     * GET /api/projects/{projectId}/parts
     * Returns all components in the rocket component tree for the given project.
     */
    @GetMapping
    public ResponseEntity<List<PartDto>> getParts(@PathVariable String projectId) {
        List<PartDto> parts = partService.getParts(projectId);
        if (parts == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(parts);
    }

    /**
     * GET /api/projects/{projectId}/parts/{partId}
     * Returns a single component by ID.
     */
    @GetMapping("/{partId}")
    public ResponseEntity<PartDto> getPart(
            @PathVariable String projectId,
            @PathVariable String partId) {
        PartDto part = partService.getPart(projectId, partId);
        if (part == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(part);
    }

    /**
     * POST /api/projects/{projectId}/parts
     * Adds a new component to the project.
     * The request body must contain at least a {@code type} field.
     */
    @PostMapping
    public ResponseEntity<?> addPart(
            @PathVariable String projectId,
            @RequestBody PartDto dto) {
        try {
            PartDto created = partService.addPart(projectId, dto);
            if (created == null) {
                return ResponseEntity.notFound().build();
            }
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{partId}")
                    .buildAndExpand(created.getId())
                    .toUri();
            return ResponseEntity.created(location).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Bad part creation request for project '{}': {}", projectId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add part to project '{}': {}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/projects/{projectId}/parts/{partId}
     * Updates the mutable properties of an existing component.
     */
    @PutMapping("/{partId}")
    public ResponseEntity<PartDto> updatePart(
            @PathVariable String projectId,
            @PathVariable String partId,
            @RequestBody PartDto dto) {
        try {
            PartDto updated = partService.updatePart(projectId, partId, dto);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update part '{}' in project '{}': {}", partId, projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /api/projects/{projectId}/parts/{partId}
     * Removes a component from the rocket component tree.
     */
    @DeleteMapping("/{partId}")
    public ResponseEntity<Void> deletePart(
            @PathVariable String projectId,
            @PathVariable String partId) {
        boolean deleted = partService.deletePart(projectId, partId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
