package com.manfred.incidenttracker.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(){
        super("Invalid email or password");
    }
}
