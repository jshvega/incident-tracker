package com.manfred.incidenttracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class ApiExceptionHandler {
    
    @ExceptionHandler
    public ProblemDetail handleIncidentNotFound(IncidentNotFoundException ex){

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

    }

}