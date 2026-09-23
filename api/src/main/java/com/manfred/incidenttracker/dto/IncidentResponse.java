package com.manfred.incidenttracker.dto;

// Defines the shape of the JSON the API returns. This is the outbound DTO.
public record IncidentResponse(Long id, String title, String severity, String status) {
    
}