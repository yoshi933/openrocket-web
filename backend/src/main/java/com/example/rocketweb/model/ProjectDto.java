package com.example.rocketweb.model;

/**
 * Data Transfer Object for a rocket project.
 * Carries essential rocket metrics alongside project metadata.
 */
public class ProjectDto {

    private String id;
    private String name;

    // Rocket metrics computed by RocketCalculator
    /** Center of Gravity position from the nose tip (meters) */
    private double cg;

    /** Center of Pressure position from the nose tip (meters) */
    private double cp;

    /** Total mass of the rocket (kilograms) */
    private double totalMass;

    /** Stability margin in calibers (positive = stable) */
    private double stabilityMargin;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCg() { return cg; }
    public void setCg(double cg) { this.cg = cg; }

    public double getCp() { return cp; }
    public void setCp(double cp) { this.cp = cp; }

    public double getTotalMass() { return totalMass; }
    public void setTotalMass(double totalMass) { this.totalMass = totalMass; }

    public double getStabilityMargin() { return stabilityMargin; }
    public void setStabilityMargin(double stabilityMargin) { this.stabilityMargin = stabilityMargin; }
}
