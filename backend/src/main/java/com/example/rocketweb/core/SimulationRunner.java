package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.FlightData;
import net.sf.openrocket.simulation.FlightDataBranch;
import net.sf.openrocket.simulation.FlightDataType;
import net.sf.openrocket.simulation.exception.SimulationException;

import com.example.rocketweb.model.SimulationResultDto;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Executes flight simulations and extracts time-series results.
 */
@Component
public class SimulationRunner {

    /**
     * Runs the simulation at the given index in the document and returns
     * altitude, velocity, and acceleration over time.
     *
     * @param document  source document
     * @param simIndex  zero-based index of the simulation to run
     * @return SimulationResultDto with time-series data and max altitude
     * @throws SimulationException if the simulation fails
     */
    public SimulationResultDto run(OpenRocketDocument document, int simIndex) throws SimulationException {
        if (document.getSimulationCount() == 0) {
            throw new IllegalStateException(
                    "No simulations defined in the document. " +
                    "Please add at least one simulation to the .ork file before running.");
        }
        if (simIndex < 0 || simIndex >= document.getSimulationCount()) {
            throw new IllegalArgumentException(
                    "Simulation index " + simIndex + " is out of range (0–" +
                    (document.getSimulationCount() - 1) + ").");
        }

        Simulation simulation = document.getSimulation(simIndex);
        simulation.simulate();

        FlightData flightData = simulation.getSimulatedData();
        FlightDataBranch branch = flightData.getBranch(0);

        List<Double> time         = branch.getValues(FlightDataType.TYPE_TIME);
        List<Double> altitude     = branch.getValues(FlightDataType.TYPE_ALTITUDE);
        List<Double> velocity     = branch.getValues(FlightDataType.TYPE_VELOCITY_TOTAL);
        List<Double> acceleration = branch.getValues(FlightDataType.TYPE_ACCELERATION_TOTAL);
        double maxAltitude        = branch.getMaximum(FlightDataType.TYPE_ALTITUDE);

        SimulationResultDto result = new SimulationResultDto();
        result.setTime(time);
        result.setAltitude(altitude);
        result.setVelocity(velocity);
        result.setAcceleration(acceleration);
        result.setMaxAltitude(maxAltitude);
        return result;
    }

    /**
     * Convenience overload that runs the first simulation (index 0).
     *
     * @param document source document
     * @return SimulationResultDto with time-series data and max altitude
     * @throws SimulationException if the simulation fails
     */
    public SimulationResultDto run(OpenRocketDocument document) throws SimulationException {
        return run(document, 0);
    }
}
