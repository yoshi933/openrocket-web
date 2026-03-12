package com.example.rocketweb.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    @PostMapping("/{projectId}/run")
    public String run(@PathVariable String projectId) {
        return "Simulation started for project: " + projectId;
    }
}
