package com.manfred.incidenttracker.exception;

public class IncidentNotFoundException extends RuntimeException {
    
    public IncidentNotFoundException(Long id){
        super("Incident " + id + " has not been found");
    }

}