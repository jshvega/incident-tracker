package com.manfred.incidenttracker.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.dto.IncidentDetail;
import com.manfred.incidenttracker.dto.IncidentResponse;
import com.manfred.incidenttracker.entity.Incident;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.IncidentNotFoundException;
import com.manfred.incidenttracker.repository.IncidentRepository;
import org.springframework.transaction.annotation.Transactional;


/* 
"Spring gives me an IncidentRepository; I use it to retrieve all Incident entities, transform each entity into an IncidentResponse DTO, collect those DTOs into a list, and return that list to whoever called the service."
*/


@Service // "This class contains application/business logic. Create an instance of it and make it available for dependency injection."
public class IncidentService {
    
    // Field
    // "This service needs an IncidentRepository to get incident data."
    private final IncidentRepository incidentRepository;

    // Constructor
    // "When Spring creates my service, give me an IncidentRepository, and I'll store it."
    public IncidentService(IncidentRepository incidentRepository){
        this.incidentRepository = incidentRepository;
    }

    // Method
    // "Give me all the incidents, but return them in the format that the outside/API layer should see."
    public List<IncidentResponse> list() {
        
        List<Incident> incidents = incidentRepository.findAll();
        // "Repository, get all the incidents from the database."

        List<IncidentResponse> results = new ArrayList<>();
        // "Builds a new list containing the responses that will eventually be returned."
        // Initially: results = []

        for (Incident item : incidents) {
            results.add(new IncidentResponse(item.getId(), item.getTitle(), item.getIncidentSeverity().name(), item.getIncidentStatus().name()));
            // "Take this incident and create an IncidentResponse containing the info I want to expose."
        }

        return results;

    }

    //Method
    @Transactional(readOnly = true)
    public IncidentDetail findById(Long id){

        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));

        User reporter = incident.getReporterId();
        User assignee = incident.getAssigneeId();

        return new IncidentDetail(
            incident.getId(),
            incident.getTitle(),
            incident.getIncidentSeverity().name(),
            incident.getIncidentStatus().name(),

            incident.getDescription(),
            incident.getSlaMinutes(),
            incident.getCreatedAt(),
            incident.getUpdatedAt(),
            incident.getResolvedAt(),
            incident.getDueAt(),
            reporter.getId(),
            reporter.getEmail(),
            assignee != null ? assignee.getId() : null,
            assignee != null ? assignee.getEmail() : null
        );

    }

}