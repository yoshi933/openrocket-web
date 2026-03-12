package com.example.rocketweb.service;

import com.example.rocketweb.core.SimulationRunner;
import com.example.rocketweb.model.SimulationResultDto;
import com.example.rocketweb.repository.ProjectRepository;

import net.sf.openrocket.document.OpenRocketDocument;
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
        OpenRocketDocument document = projectRepository.findDocumentById(projectId);
        if (document == null) {
            return null;
        }
        SimulationResultDto result = simulationRunner.run(document);
        result.setProjectId(projectId);
        return result;
    }
}
