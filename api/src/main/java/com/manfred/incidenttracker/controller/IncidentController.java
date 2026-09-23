package com.manfred.incidenttracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.manfred.incidenttracker.dto.IncidentDetail;
import com.manfred.incidenttracker.dto.IncidentResponse;
import com.manfred.incidenttracker.service.IncidentService;


/*
TASK
Replace the hardcoded list. Same pattern as the service: a private final IncidentService field, constructor injection, and the method now returns whatever the service hands back. The controller should end up doing almost nothing, and that's correct. Its job is HTTP, not logic.

The rule this enforces: the controller never touches the repository and never sees an Incident. Controller → service → repository, one direction.
*/


// CLASS
@RestController
@RequestMapping("/incidents") // Sets a path. Everything inside starts with /incidents.
public class IncidentController {

    // FIELD
    private final IncidentService incidentService;

    // CONSTRUCTOR
    public IncidentController(IncidentService incidentService){
        this.incidentService = incidentService;
    }

    // METHODS
    @GetMapping 
    // Passed nothing. If I later wrote @GetMapping("/open"), it would serve /incidents/open.
    public List<IncidentResponse> list() {
        return incidentService.list();
    }

    @GetMapping("/{id}")
    public IncidentDetail detail(@PathVariable Long id){
        return incidentService.findById(id);
    }

}