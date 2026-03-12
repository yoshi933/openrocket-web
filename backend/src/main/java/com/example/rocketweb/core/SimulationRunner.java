package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.simulation.exception.SimulationException;

import com.example.rocketweb.model.SimulationResultDto;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes flight simulations and extracts time-series results.
 */
@Component
public class SimulationRunner {

    /**
     * Runs the first available simulation in the document and returns
     * altitude, velocity, and acceleration over time.
     *
     * @param document source document
     * @return SimulationResultDto with time-series data
     * @throws SimulationException if the simulation fails
     */
    public SimulationResultDto run(OpenRocketDocument document) throws SimulationException {
        if (document.getSimulationCount() == 0) {
            throw new IllegalStateException(
                    "No simulations defined in the document. " +
                    "Please add at least one simulation to the .ork file before running.");
        }

        Simulation simulation = document.getSimulation(0);
        simulation.simulate();

        List<Double> time = new ArrayList<>();
        List<Double> altitude = new ArrayList<>();
        List<Double> velocity = new ArrayList<>();
        List<Double> acceleration = new ArrayList<>();

        for (SimulationStatus status : simulation.getSimulatedData().getBranch(0).getDataPoints()) {
            time.add(status.getSimulationTime());
            altitude.add(status.getRocketPosition().z);
            velocity.add(status.getRocketVelocity().length());
            acceleration.add(status.getRocketAcceleration().length());
        }

        SimulationResultDto result = new SimulationResultDto();
        result.setTime(time);
        result.setAltitude(altitude);
        result.setVelocity(velocity);
        result.setAcceleration(acceleration);
        return result;
    }
}
