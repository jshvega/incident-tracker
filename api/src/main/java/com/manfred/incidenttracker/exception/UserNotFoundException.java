package com.manfred.incidenttracker.exception;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(Long id){
        super("User " + id + " has not been found.");
    }

}
