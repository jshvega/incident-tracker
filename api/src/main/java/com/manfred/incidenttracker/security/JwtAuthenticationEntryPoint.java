package com.manfred.incidenttracker.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.security.core.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component 
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public JwtAuthenticationEntryPoint(JsonMapper jsonMapper){
        this.jsonMapper = jsonMapper;
    }

    @Override 
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException{

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");

        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(401);

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        jsonMapper.writeValue(response.getOutputStream(), problem);

    }
    
}
