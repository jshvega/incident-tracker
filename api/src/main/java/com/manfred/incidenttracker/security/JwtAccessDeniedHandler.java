package com.manfred.incidenttracker.security;

import java.io.IOException;
import java.net.URI;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component 
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JwtAccessDeniedHandler(JsonMapper jsonMapper){
        this.jsonMapper = jsonMapper;
    }

    @Override 
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException{

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");

        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(403);

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        jsonMapper.writeValue(response.getOutputStream(), problem);

    }
    
}
