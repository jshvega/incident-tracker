package com.manfred.incidenttracker.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import com.manfred.incidenttracker.entity.Status;
import com.manfred.incidenttracker.exception.IllegalTransitionException;

public class IncidentStateMachine {
 
    // TABLE
    private final static Map<Status, Set<Status>> transitions = new EnumMap<>(Status.class);
    static {
        transitions.put(Status.open,            EnumSet.of(Status.investigating, Status.closed));
        transitions.put(Status.investigating,   EnumSet.of(Status.resolved));
        transitions.put(Status.resolved,        EnumSet.of(Status.closed, Status.investigating));
        transitions.put(Status.closed,          EnumSet.noneOf(Status.class));
    }

    // METHODS
    public boolean isTransitionAllowed(Status from, Status to){
        return transitions.get(from).contains(to);
    }

    public void validateTransition(Status from, Status to){
        if(!isTransitionAllowed(from, to)){
            throw new IllegalTransitionException(from, to);
        }
    }

}
