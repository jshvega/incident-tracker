package com.manfred.incidenttracker.exception;

import com.manfred.incidenttracker.entity.Role;

public class InvalidAssigneeException extends RuntimeException {
    
    public InvalidAssigneeException(Long userId, Role role){
        super("User " + userId + " has role "+ role + " and can't be assigned.");
    }

}
