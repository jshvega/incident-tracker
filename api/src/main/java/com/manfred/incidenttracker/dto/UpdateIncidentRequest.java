package com.manfred.incidenttracker.dto;

import com.manfred.incidenttracker.entity.Severity;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateIncidentRequest(
    @Pattern (regexp = ".*\\S.*") @Size(max = 50) String title,
    @Pattern (regexp = "(?s).*\\S.*") @Size(max = 250) String description,
    Severity severity
) {
}
