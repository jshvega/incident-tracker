package com.manfred.incidenttracker.dto;

import jakarta.validation.constraints.Positive;

public record AssignIncidentRequest(
    @Positive  Long assigneeId
) {
}