package com.manfred.incidenttracker.dto;

import java.time.OffsetDateTime;

public record IncidentDetail(
    Long id,
    String title, 
    String severity, 
    String status,

    String description, 
    int slaMinutes, 
    OffsetDateTime createdAt, 
    OffsetDateTime updatedAt, 
    OffsetDateTime resolvedAt, 
    OffsetDateTime dueAt, 
    UserSummary reporter, 
    UserSummary assignee){
}
