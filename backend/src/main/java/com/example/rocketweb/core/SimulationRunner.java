package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.Simulation;
import org.springframework.stereotype.Component;

@Component
public class SimulationRunner {

    /**
     * Runs the first simulation in the given OpenRocketDocument.
     *
     * @param doc the OpenRocketDocument containing simulation definitions
     * @throws IllegalArgumentException if the document has no simulations
     * @throws Exception if the simulation fails
     */
    public void runSimulation(OpenRocketDocument doc) throws Exception {
        if (doc.getSimulationCount() == 0) {
            throw new IllegalArgumentException("No simulations found in the document.");
        }
        Simulation sim = doc.getSimulation(0);
        sim.simulate();
    }
}
