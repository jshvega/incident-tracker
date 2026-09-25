package com.manfred.incidenttracker.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class ApiExceptionHandler {
    
    @ExceptionHandler
    public ProblemDetail handleIncidentNotFound(IncidentNotFoundException ex){

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

    }

    @ExceptionHandler 
    public ProblemDetail handleUserNotFound(UserNotFoundException ex){

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

    }

    @ExceptionHandler 
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex){

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()){
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setProperty("errors", errors);

        return problem;

    }

    @ExceptionHandler 
    public ProblemDetail handleIllegalTransition(IllegalTransitionException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

}