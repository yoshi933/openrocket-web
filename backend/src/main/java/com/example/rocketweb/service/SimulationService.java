package com.example.rocketweb.service;

import com.example.rocketweb.core.SimulationRunner;
import com.example.rocketweb.model.SimulationResultDto;
import com.example.rocketweb.repository.ProjectRepository;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.SimulationOptions;
import net.sf.openrocket.simulation.exception.SimulationException;

import org.springframework.stereotype.Service;

/**
 * Business logic for running flight simulations.
 */
@Service
public class SimulationService {

    private final ProjectRepository projectRepository;
    private final SimulationRunner simulationRunner;

    public SimulationService(ProjectRepository projectRepository,
                             SimulationRunner simulationRunner) {
        this.projectRepository = projectRepository;
        this.simulationRunner = simulationRunner;
    }

    /**
     * Runs the first simulation in the project with the given ID.
     *
     * @param projectId project to simulate
     * @return time-series results, or null if the project does not exist
     * @throws SimulationException if the simulation fails
     */
    public SimulationResultDto runSimulation(String projectId) throws SimulationException {
        return runSimulation(projectId, 0, null, null, null);
    }

    /**
     * Runs the simulation at the specified index, optionally overriding launch
     * parameters before execution.
     *
     * @param projectId       project to simulate
     * @param simIndex        zero-based index of the simulation within the document
     * @param launchRodLength launch rod length in meters, or null to keep current value
     * @param launchAngle     launch rod angle from vertical in degrees, or null to keep current value
     * @param windSpeed       average wind speed in m/s, or null to keep current value
     * @return time-series results, or null if the project does not exist
     * @throws SimulationException if the simulation fails
     */
    public SimulationResultDto runSimulation(String projectId,
                                             int simIndex,
                                             Double launchRodLength,
                                             Double launchAngle,
                                             Double windSpeed) throws SimulationException {
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }

        // Validate simIndex early so parameter application and simulation use the same guard
        if (simIndex < 0 || simIndex >= document.getSimulationCount()) {
            throw new IllegalArgumentException(
                    "Simulation index " + simIndex + " is out of range (0–" +
                    (document.getSimulationCount() - 1) + ").");
        }

        // Validate physical parameters before applying them
        if (launchRodLength != null && launchRodLength <= 0) {
            throw new IllegalArgumentException("launchRodLength must be positive, got: " + launchRodLength);
        }
        if (launchAngle != null && (launchAngle < -180 || launchAngle > 180)) {
            throw new IllegalArgumentException("launchAngle must be between -180 and 180 degrees, got: " + launchAngle);
        }
        if (windSpeed != null && windSpeed < 0) {
            throw new IllegalArgumentException("windSpeed must be non-negative, got: " + windSpeed);
        }

        Simulation simulation = document.getSimulation(simIndex);
        SimulationOptions options = simulation.getOptions();

        if (launchRodLength != null) {
            options.setLaunchRodLength(launchRodLength);
        }
        if (launchAngle != null) {
            options.setLaunchRodAngle(Math.toRadians(launchAngle));
        }
        if (windSpeed != null) {
            options.setWindSpeedAverage(windSpeed);
        }

        SimulationResultDto result = simulationRunner.run(document, simIndex);
        result.setProjectId(projectId);
        return result;
    }
}
