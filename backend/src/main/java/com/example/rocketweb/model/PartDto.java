package com.example.rocketweb.model;

/**
 * Data Transfer Object for a single rocket component (part).
 * Carries the essential properties needed by the frontend component tree and editor.
 */
public class PartDto {

    /** Unique component ID assigned by OpenRocket-core. */
    private String id;

    /** Human-readable component name. */
    private String name;

    /**
     * Component type string (Java class simple name, e.g. "NoseCone", "BodyTube").
     * Used by the frontend to determine which editor fields to show.
     */
    private String type;

    /** ID of the parent component, or null if this is the rocket root. */
    private String parentId;

    /** Axial length of the component in meters. */
    private double length;

    /** Outer diameter of the component in meters (0 for components without a diameter). */
    private double diameter;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }

    public double getDiameter() { return diameter; }
    public void setDiameter(double diameter) { this.diameter = diameter; }
}
