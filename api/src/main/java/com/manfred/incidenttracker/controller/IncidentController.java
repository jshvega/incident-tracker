package com.manfred.incidenttracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import com.manfred.incidenttracker.dto.AssignIncidentRequest;
import com.manfred.incidenttracker.dto.CommentResponse;
import com.manfred.incidenttracker.dto.CreateCommentRequest;
import com.manfred.incidenttracker.dto.CreateIncidentRequest;
import com.manfred.incidenttracker.dto.IncidentDetail;
import com.manfred.incidenttracker.dto.IncidentResponse;
import com.manfred.incidenttracker.dto.StatusHistoryEntry;
import com.manfred.incidenttracker.dto.TransitionRequest;
import com.manfred.incidenttracker.dto.UpdateIncidentRequest;
import com.manfred.incidenttracker.service.IncidentService;

import jakarta.validation.Valid;


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

    @PostMapping 
    public ResponseEntity<IncidentDetail> create(@Valid @RequestBody CreateIncidentRequest req, @RequestHeader("X-User-Id") Long userId){

        IncidentDetail detail = incidentService.create(req, userId);

        URI location = URI.create("/incidents/"+detail.id());

        return ResponseEntity.created(location).body(detail);

    }

    @PostMapping("/{id}/transitions")
    public IncidentDetail transitions(
    @PathVariable Long id,
    @Valid @RequestBody TransitionRequest transitionRequest,
    @RequestHeader("X-User-Id") Long userId) {

        IncidentDetail detail = incidentService.transition(id, transitionRequest.status(), userId);

        return detail;

    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryEntry> history(@PathVariable Long id){
        return incidentService.history(id);
    }

    @PatchMapping("/{id}")
    public IncidentDetail update(@PathVariable Long id, @Valid @RequestBody UpdateIncidentRequest req){

        IncidentDetail update = incidentService.update(id, req);

        return update;

    }

    @PatchMapping("/{id}/assignee")
    public IncidentDetail assign(@PathVariable Long id, @Valid @RequestBody AssignIncidentRequest req){

        IncidentDetail assign = incidentService.assign(id, req);

        return assign;

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){

        incidentService.delete(id);

        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> comment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest req, @RequestHeader("X-User-Id") Long userId){

        CommentResponse comment = incidentService.addComment(id, req, userId);

        URI location = URI.create("/incidents/" + id + "/comments");
        // the collection URL is the real place a client can go to read the comment back

        return ResponseEntity.created(location).body(comment);

    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id){
        return incidentService.comments(id);
    }

}