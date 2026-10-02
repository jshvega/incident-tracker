package com.manfred.incidenttracker.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.manfred.incidenttracker.entity.Role;
import com.manfred.incidenttracker.entity.Status;
import com.manfred.incidenttracker.exception.IllegalTransitionException;

@Component 
public class IncidentStateMachine {

    // FIELDS
    private static final Set<Role> STAFF = EnumSet.of(Role.assignee, Role.admin);
    private static final Set<Role> ADMIN_ONLY = EnumSet.of(Role.admin);
 
    // TABLE
    private final static Map<Status, Map<Status, Set<Role>>> transitions = new EnumMap<>(Status.class);
    static {

        // From OPEN
        transitions.put(Status.open, new EnumMap<>(Map.of(
            Status.investigating, STAFF,
            Status.closed, ADMIN_ONLY
        )));

        // From INVESTIGATING
        transitions.put(Status.investigating, new EnumMap<>(Map.of(
            Status.resolved, STAFF
        )));

        // From RESOLVED
        transitions.put(Status.resolved, new EnumMap<>(Map.of(
            Status.closed, STAFF,
            Status.investigating, STAFF
        )));

        // From CLOSED
        transitions.put(Status.closed, new EnumMap<>(Status.class));

    }

    // CONSTRUCTOR
    public IncidentStateMachine(){}

    // METHODS
    public boolean isTransitionAllowed(Status from, Status to){
        return transitions.get(from).containsKey(to);
    }

    public void validateTransition(Status from, Status to){
        if(!isTransitionAllowed(from, to)){
            throw new IllegalTransitionException(from, to);
        }
    }

    public boolean isRoleAllowed(Status from, Status to, Role role){

        return transitions.get(from).getOrDefault(to, Set.of()).contains(role);
    }

}
