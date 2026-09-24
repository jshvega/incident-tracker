package com.manfred.incidenttracker.exception;

import com.manfred.incidenttracker.entity.Status;

public class IllegalTransitionException extends RuntimeException {
    
    public IllegalTransitionException(Status from, Status to){
        super("Going from " + from + " to " + to + " is not allowed.");
    }

}
