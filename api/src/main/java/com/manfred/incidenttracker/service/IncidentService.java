package com.manfred.incidenttracker.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.domain.IncidentStateMachine;
import com.manfred.incidenttracker.dto.AssignIncidentRequest;
import com.manfred.incidenttracker.dto.CreateIncidentRequest;
import com.manfred.incidenttracker.dto.IncidentDetail;
import com.manfred.incidenttracker.dto.IncidentResponse;
import com.manfred.incidenttracker.dto.StatusHistoryEntry;
import com.manfred.incidenttracker.dto.UpdateIncidentRequest;
import com.manfred.incidenttracker.entity.Incident;
import com.manfred.incidenttracker.entity.Status;
import com.manfred.incidenttracker.entity.StatusHistory;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.IncidentNotFoundException;
import com.manfred.incidenttracker.exception.UserNotFoundException;
import com.manfred.incidenttracker.repository.IncidentRepository;
import com.manfred.incidenttracker.repository.StatusHistoryRepository;
import com.manfred.incidenttracker.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;


/* 
"Spring gives me an IncidentRepository; I use it to retrieve all Incident entities, transform each entity into an IncidentResponse DTO, collect those DTOs into a list, and return that list to whoever called the service."
*/


@Service // "This class contains application/business logic. Create an instance of it and make it available for dependency injection."
public class IncidentService {
    
    // FIELDS
    // "This service needs an IncidentRepository to get incident data."
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final IncidentStateMachine machine;

    // CONSTRUCTOR
    // "When Spring creates my service, give me an IncidentRepository, and I'll store it."
    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository, StatusHistoryRepository statusHistoryRepository, IncidentStateMachine machine){
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.machine = machine;
    }

    // METHOD
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

    //METHOD
    private IncidentDetail toDetail(Incident incident){

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

    //METHOD
    @Transactional(readOnly = true)
    public IncidentDetail findById(Long id){

        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));

        return toDetail(incident);

    }

    //METHOD
    @Transactional 
    public IncidentDetail create(CreateIncidentRequest req, Long reporterId){

        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new UserNotFoundException(reporterId));

        User assignee = null;
        if(req.assigneeId() != null){
            assignee = userRepository.findById(req.assigneeId()).orElseThrow(() -> new UserNotFoundException(req.assigneeId()));
        }

        OffsetDateTime dueAt = OffsetDateTime.now().plusMinutes(req.slaMinutes());

        Incident incident = new Incident(req.title(), req.description(), req.severity(), reporter, req.slaMinutes(), dueAt);
        incident.setAssignee(assignee);

        Incident saved = incidentRepository.save(incident);

        return toDetail(saved);

    }

    //METHOD
    @Transactional 
    public IncidentDetail transition(Long incidentId, Status to, Long userId){

        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        machine.validateTransition(incident.getIncidentStatus(), to);

        Status from = incident.getIncidentStatus();

        incident.setIncidentStatus(to);

        if(to == Status.resolved){
            incident.setResolvedAt(OffsetDateTime.now());
        } else if(from == Status.resolved && to == Status.investigating){
            incident.setResolvedAt(null);
        }

        StatusHistory historyRow = new StatusHistory(incident, from, to, user);

        statusHistoryRepository.save(historyRow);

        incidentRepository.flush();
        //There is a chance this might not be needed. Adding it for safety.

        return toDetail(incident);

    }

    //METHOD
    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> history(Long incidentId){

        if(!incidentRepository.existsById(incidentId)){
            throw new IncidentNotFoundException(incidentId);
        }

        List<StatusHistory> rows = statusHistoryRepository.findByIncidentIdOrderByChangedAtAsc(incidentId);

        List<StatusHistoryEntry> results = new ArrayList<>();

        for (StatusHistory item : rows){

            User user = item.getChangedBy();

            results.add(new StatusHistoryEntry(
                item.getFromStatus().name(), 
                item.getToStatus().name(), 
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                item.getChangedAt())
            );
        }

        return results;

    }

    //METHOD
    @Transactional 
    public IncidentDetail update(Long incidentId, UpdateIncidentRequest reqUpdate){

        // Load the incident, or throw error.
        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if(reqUpdate.title() != null){
            incident.setIncidentTitle(reqUpdate.title());
        }
        if(reqUpdate.description() != null){
            incident.setIncidentDescription(reqUpdate.description());
        }
        if(reqUpdate.severity() != null){
            incident.setIncidentSeverity(reqUpdate.severity());
        }

        incidentRepository.flush();
        //There is a chance this might not be needed. Adding it for safety.

        return toDetail(incident);
    }

    //METHOD
    @Transactional 
    public IncidentDetail assign(Long incidentId, AssignIncidentRequest req){

        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));

        User assignee = null;

        if(req.assigneeId() != null){
            assignee = userRepository.findById(req.assigneeId()).orElseThrow(() -> new UserNotFoundException(req.assigneeId()));
        }

        incident.setAssignee(assignee);

        incidentRepository.flush();

        return toDetail(incident);

    }

    //METHOD
    @Transactional 
    public void delete(Long incidentId){
        
        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));

        incidentRepository.delete(incident);

    }

}