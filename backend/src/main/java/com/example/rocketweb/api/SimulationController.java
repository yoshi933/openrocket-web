package com.example.rocketweb.api;

import com.example.rocketweb.model.SimulationResultDto;
import com.example.rocketweb.service.SimulationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for simulation execution.
 */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * POST /api/simulations/{projectId}/run
     * Runs the first simulation in the project and returns time-series results.
     */
    @PostMapping("/{projectId}/run")
    public ResponseEntity<SimulationResultDto> run(@PathVariable String projectId) {
        try {
            SimulationResultDto result = simulationService.runSimulation(projectId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Simulation failed for project '{}': {}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
