package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.rocketcomponent.BodyTube;
import net.sf.openrocket.rocketcomponent.NoseCone;
import net.sf.openrocket.rocketcomponent.Rocket;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.rocketcomponent.Transition;
import net.sf.openrocket.rocketcomponent.FinSet;
import net.sf.openrocket.rocketcomponent.TrapezoidFinSet;
import net.sf.openrocket.rocketcomponent.FlightConfigurationId;
import net.sf.openrocket.rocketcomponent.RocketStatus;

import org.springframework.stereotype.Component;

@Component
public class RocketCalculator {

    /**
     * Create a minimal rocket and compute CG/CP.
     * This is the smallest working example using OpenRocket-core.
     */
    public Summary calculateMinimalRocket() {

        // 1. Create a rocket
        Rocket rocket = new Rocket();

        // 2. Add a nose cone
        NoseCone nose = new NoseCone();
        nose.setLength(0.10); // 10 cm
        nose.setOuterRadius(0.02); // 2 cm radius
        rocket.addChild(nose);

        // 3. Add a body tube
        BodyTube tube = new BodyTube();
        tube.setLength(0.30); // 30 cm
        tube.setOuterRadius(0.02);
        tube.setInnerRadius(0.018);
        rocket.addChild(tube);

        // 4. Add a simple fin set
        TrapezoidFinSet fins = new TrapezoidFinSet();
        fins.setFinCount(3);
        fins.setRootChord(0.05);
        fins.setTipChord(0.03);
        fins.setSpan(0.04);
        tube.addChild(fins);

        // 5. Get default flight configuration
        FlightConfigurationId config = rocket.getDefaultConfiguration().getFlightConfigurationID();

        // 6. Compute rocket status (CG, CP, mass, etc.)
        RocketStatus status = rocket.getStatus(config);

        // 7. Return summary
        Summary summary = new Summary();
        summary.cg = status.getCG();
        summary.cp = status.getCP();
        summary.mass = status.getMass();
        summary.length = rocket.getLength();

        return summary;
    }

    // Simple DTO for returning results
    public static class Summary {
        public double cg;
        public double cp;
        public double mass;
        public double length;
    }
}

