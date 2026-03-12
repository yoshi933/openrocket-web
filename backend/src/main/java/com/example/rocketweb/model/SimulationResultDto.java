package com.example.rocketweb.model;

import java.util.List;

/**
 * Time-series simulation results returned to the frontend.
 */
public class SimulationResultDto {

    private String projectId;

    /** Time values in seconds */
    private List<Double> time;

    /** Altitude values in meters */
    private List<Double> altitude;

    /** Velocity values in meters per second */
    private List<Double> velocity;

    /** Acceleration values in meters per second squared */
    private List<Double> acceleration;

    /** Maximum altitude reached during the flight (meters) */
    private double maxAltitude;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public List<Double> getTime() { return time; }
    public void setTime(List<Double> time) { this.time = time; }

    public List<Double> getAltitude() { return altitude; }
    public void setAltitude(List<Double> altitude) { this.altitude = altitude; }

    public List<Double> getVelocity() { return velocity; }
    public void setVelocity(List<Double> velocity) { this.velocity = velocity; }

    public List<Double> getAcceleration() { return acceleration; }
    public void setAcceleration(List<Double> acceleration) { this.acceleration = acceleration; }

    public double getMaxAltitude() { return maxAltitude; }
    public void setMaxAltitude(double maxAltitude) { this.maxAltitude = maxAltitude; }
}
