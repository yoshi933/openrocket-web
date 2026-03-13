package com.example.rocketweb.service;

import com.example.rocketweb.model.PartDto;
import com.example.rocketweb.repository.ProjectRepository;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.rocketcomponent.BodyComponent;
import net.sf.openrocket.rocketcomponent.BodyTube;
import net.sf.openrocket.rocketcomponent.MassComponent;
import net.sf.openrocket.rocketcomponent.NoseCone;
import net.sf.openrocket.rocketcomponent.Parachute;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.rocketcomponent.SymmetricComponent;
import net.sf.openrocket.rocketcomponent.Transition;
import net.sf.openrocket.rocketcomponent.TrapezoidFinSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for managing rocket parts (components).
 * Reads and mutates the OpenRocketDocument stored in the repository.
 */
@Service
public class PartService {

    private static final Logger log = LoggerFactory.getLogger(PartService.class);

    private final ProjectRepository projectRepository;

    public PartService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Returns all components in the rocket component tree for the given project.
     * The list is ordered by depth-first traversal.
     *
     * @param projectId project ID
     * @return list of PartDtos, or null if the project is not found
     */
    public List<PartDto> getParts(String projectId) {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }
        List<PartDto> parts = new ArrayList<>();
        traverseComponent(document.getRocket(), parts);
        return parts;
    }

    /**
     * Returns a single component identified by {@code partId}.
     *
     * @param projectId project ID
     * @param partId    component ID
     * @return PartDto, or null if either the project or the component is not found
     */
    public PartDto getPart(String projectId, String partId) {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }
        RocketComponent component = findComponentById(document.getRocket(), partId);
        if (component == null) {
            return null;
        }
        return toDto(component);
    }

    /**
     * Adds a new component of the type specified in the DTO to the given parent.
     * The {@code parentId} field in the DTO identifies the parent component;
     * if it is null the rocket root is used.
     *
     * @param projectId project ID
     * @param dto       PartDto containing at least {@code type} and optionally {@code parentId},
     *                  {@code name}, {@code length}, and {@code diameter}
     * @return PartDto for the newly created component, or null if the project or
     *         parent is not found
     * @throws IllegalArgumentException if the type is unsupported or the component
     *         cannot be added to the specified parent
     */
    public PartDto addPart(String projectId, PartDto dto) {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }

        RocketComponent parent;
        if (dto.getParentId() == null) {
            parent = document.getRocket();
        } else {
            parent = findComponentById(document.getRocket(), dto.getParentId());
            if (parent == null) {
                return null;
            }
        }

        RocketComponent newComponent = createComponent(dto.getType());
        if (newComponent == null) {
            throw new IllegalArgumentException("Unsupported component type: " + dto.getType());
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            newComponent.setName(dto.getName());
        }
        if (dto.getLength() > 0 && newComponent instanceof BodyComponent bc) {
            bc.setLength(dto.getLength());
        }
        if (dto.getDiameter() > 0) {
            setDiameter(newComponent, dto.getDiameter());
        }

        try {
            parent.addChild(newComponent);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot add {} to {}: {}", dto.getType(), parent.getClass().getSimpleName(), e.getMessage());
            throw e;
        }

        log.info("Added {} '{}' to project '{}'", dto.getType(), newComponent.getName(), projectId);
        return toDto(newComponent);
    }

    /**
     * Updates the mutable properties of an existing component.
     * Only non-null / positive values are applied; omitted fields are left unchanged.
     *
     * @param projectId project ID
     * @param partId    component ID
     * @param dto       PartDto containing fields to update
     * @return updated PartDto, or null if the project or component is not found
     */
    public PartDto updatePart(String projectId, String partId, PartDto dto) {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }
        RocketComponent component = findComponentById(document.getRocket(), partId);
        if (component == null) {
            return null;
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            component.setName(dto.getName());
        }
        if (dto.getLength() > 0 && component instanceof BodyComponent bc) {
            bc.setLength(dto.getLength());
        }
        if (dto.getDiameter() > 0) {
            setDiameter(component, dto.getDiameter());
        }

        log.info("Updated part '{}' in project '{}'", partId, projectId);
        return toDto(component);
    }

    /**
     * Removes a component from the rocket component tree.
     *
     * @param projectId project ID
     * @param partId    component ID
     * @return true if the component was found and removed; false if the project or
     *         component was not found, or the component has no parent (i.e. is root)
     */
    public boolean deletePart(String projectId, String partId) {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return false;
        }
        RocketComponent component = findComponentById(document.getRocket(), partId);
        if (component == null) {
            return false;
        }
        RocketComponent parent = component.getParent();
        if (parent == null) {
            log.warn("Cannot delete root component '{}' in project '{}'", partId, projectId);
            return false;
        }
        parent.removeChild(component);
        log.info("Deleted part '{}' from project '{}'", partId, projectId);
        return true;
    }

    // --- helpers ---

    /** Depth-first traversal that appends every component to {@code result}. */
    private void traverseComponent(RocketComponent component, List<PartDto> result) {
        result.add(toDto(component));
        for (RocketComponent child : component.getChildren()) {
            traverseComponent(child, result);
        }
    }

    /** Finds a component by its OpenRocket ID using depth-first search. */
    private RocketComponent findComponentById(RocketComponent root, String id) {
        if (root.getID().equals(id)) {
            return root;
        }
        for (RocketComponent child : root.getChildren()) {
            RocketComponent found = findComponentById(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Converts a RocketComponent to a PartDto. */
    private PartDto toDto(RocketComponent component) {
        PartDto dto = new PartDto();
        dto.setId(component.getID());
        dto.setName(component.getName());
        dto.setType(component.getClass().getSimpleName());
        dto.setLength(component.getLength());

        RocketComponent parent = component.getParent();
        if (parent != null) {
            dto.setParentId(parent.getID());
        }

        if (component instanceof SymmetricComponent sym) {
            dto.setDiameter(sym.getAftRadius() * 2.0);
        }

        return dto;
    }

    /**
     * Sets the outer diameter of a symmetric component using the most specific setter
     * available in the OpenRocket 22.02 API:
     * <ul>
     *   <li>{@link BodyTube} → {@code setOuterRadius}</li>
     *   <li>{@link Transition} (includes {@link NoseCone}) → {@code setAftRadius}</li>
     *   <li>Other types → no-op</li>
     * </ul>
     */
    private void setDiameter(RocketComponent component, double diameter) {
        double radius = diameter / 2.0;
        if (component instanceof BodyTube bt) {
            bt.setOuterRadius(radius);
        } else if (component instanceof Transition tr) {
            tr.setAftRadius(radius);
        }
        // Other component types do not expose a simple radius setter here
    }

    /**
     * Instantiates a new rocket component based on the given type string.
     * Returns null for unsupported types.
     */
    private RocketComponent createComponent(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "NoseCone"          -> new NoseCone();
            case "BodyTube"          -> new BodyTube();
            case "Transition"        -> new Transition();
            case "TrapezoidFinSet"   -> new TrapezoidFinSet();
            case "MassComponent"     -> new MassComponent();
            case "Parachute"         -> new Parachute();
            default                  -> null;
        };
    }
}
