package com.manfred.incidenttracker.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.manfred.incidenttracker.dto.IncidentResponse;

// CLASS
@RestController
@RequestMapping("/incidents") // Sets a path. Everything inside starts with /incidents.
public class IncidentController {

    // METHOD
    @GetMapping // Passed nothing. If I later wrote @GetMapping("/open"), that method would serve /incidents/open.
    public List<IncidentResponse> list() {

        return List.of(
            new IncidentResponse(1L, "API down", "critical", "open"),
            new IncidentResponse(2L, "UI bug", "low", "open"),
            new IncidentResponse(3L, "Slow connection", "high", "investigating")
        );

    }

}