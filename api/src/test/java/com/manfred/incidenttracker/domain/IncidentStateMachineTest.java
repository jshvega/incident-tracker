package com.manfred.incidenttracker.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.manfred.incidenttracker.entity.Status;
import com.manfred.incidenttracker.exception.IllegalTransitionException;

public class IncidentStateMachineTest {
    
    private final IncidentStateMachine machine = new IncidentStateMachine();
    
    @Test
    void openToInvestigatingIsAllowed(){
        assertDoesNotThrow(() -> machine.validateTransition(Status.open, Status.investigating));
    } 
    @Test
    void openToClosedIsAllowed(){
        assertDoesNotThrow(() -> machine.validateTransition(Status.open, Status.closed));
    } 
    @Test
    void investigatingToResolvedIsAllowed(){
        assertDoesNotThrow(() -> machine.validateTransition(Status.investigating, Status.resolved));
    } 
    @Test
    void resolvedToClosedIsAllowed(){
        assertDoesNotThrow(() -> machine.validateTransition(Status.resolved, Status.closed));
    } 
    // Reopen
    @Test
    void resolvedToInvestigatingIsAllowed(){
        assertDoesNotThrow(() -> machine.validateTransition(Status.resolved, Status.investigating));
    }

    @Test
    void openToResolvedThrows(){
        assertThrows(IllegalTransitionException.class, () -> machine.validateTransition(Status.open, Status.resolved));
    }
    @Test
    void closedToInvestigatingThrows(){
        assertThrows(IllegalTransitionException.class, () -> machine.validateTransition(Status.closed, Status.investigating));
    }
    @Test
    void closedToOpenThrows(){
        assertThrows(IllegalTransitionException.class, () -> machine.validateTransition(Status.closed, Status.open));
    }
    @Test
    void openToOpenThrows(){
        assertThrows(IllegalTransitionException.class, () -> machine.validateTransition(Status.open, Status.open));
    }

}
