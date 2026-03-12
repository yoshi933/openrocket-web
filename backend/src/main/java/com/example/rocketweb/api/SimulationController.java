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
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * GET /api/projects/{projectId}/simulations/{simIndex}/results
     * Executes the simulation at the given index and returns time-series results
     * as JSON suitable for graph rendering in the frontend.
     *
     * Optional query parameters allow overriding simulation options:
     * <ul>
     *   <li>{@code launchRodLength} – launch rod length in meters</li>
     *   <li>{@code launchAngle}     – launch angle from vertical in degrees</li>
     *   <li>{@code windSpeed}       – average wind speed in m/s</li>
     * </ul>
     */
    @GetMapping("/api/projects/{projectId}/simulations/{simIndex}/results")
    public ResponseEntity<SimulationResultDto> getResults(
            @PathVariable String projectId,
            @PathVariable int simIndex,
            @RequestParam(required = false) Double launchRodLength,
            @RequestParam(required = false) Double launchAngle,
            @RequestParam(required = false) Double windSpeed) {
        try {
            SimulationResultDto result = simulationService.runSimulation(
                    projectId, simIndex, launchRodLength, launchAngle, windSpeed);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Bad simulation request for project '{}', index {}: {}", projectId, simIndex, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Simulation failed for project '{}', index {}: {}", projectId, simIndex, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/simulations/{projectId}/run
     * Runs the first simulation in the project and returns time-series results.
     *
     * @deprecated Use {@code GET /api/projects/{projectId}/simulations/{simIndex}/results} instead.
     */
    @PostMapping("/api/simulations/{projectId}/run")
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
