package com.example.rocketweb.core;

import net.sf.openrocket.aerodynamics.AerodynamicCalculator;
import net.sf.openrocket.aerodynamics.BarrowmanCalculator;
import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.aerodynamics.AerodynamicForces;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.masscalc.MassCalculator;
import net.sf.openrocket.masscalc.RigidBody;
import net.sf.openrocket.rocketcomponent.FlightConfiguration;
import net.sf.openrocket.rocketcomponent.Rocket;
import net.sf.openrocket.util.Coordinate;

import com.example.rocketweb.model.ProjectDto;

import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Computes rocket metrics (CG, CP, mass, stability) from an OpenRocketDocument
 * using OpenRocket-core's aerodynamic and mass calculators.
 */
@Component
public class RocketCalculator {

    /**
     * Calculates CG, CP, total mass and stability margin for the default flight
     * configuration of the supplied document and writes the values into the DTO.
     *
     * @param document source OpenRocketDocument
     * @param dto      DTO to populate with calculated metrics
     */
    public void calculate(OpenRocketDocument document, ProjectDto dto) {
        Rocket rocket = document.getRocket();
        FlightConfiguration config = rocket.getDefaultConfiguration();

        // --- Mass / CG ---
        RigidBody structuralMass = MassCalculator.calculateStructure(config);
        Coordinate cg = structuralMass.getCenterOfMass();
        dto.setCg(cg.x);
        dto.setTotalMass(structuralMass.getMass());

        // --- CP (Barrowman method, Mach 0, α 0) ---
        AerodynamicCalculator aeroCalc = new BarrowmanCalculator();
        FlightConditions conditions = new FlightConditions(config);
        conditions.setMach(0.3);
        conditions.setAOA(0.0);

        AerodynamicForces forces = aeroCalc.getAerodynamicForces(config, conditions, null, new HashMap<>());
        double cpX = forces.getCP().x;
        dto.setCp(cpX);

        // --- Stability margin in calibers ---
        double refDiameter = config.getReferenceLength();
        if (refDiameter > 0) {
            dto.setStabilityMargin((cpX - cg.x) / refDiameter);
        }
    }
}
