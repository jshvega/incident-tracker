package com.manfred.incidenttracker.dto;

import com.manfred.incidenttracker.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
    @NotBlank @Size(max = 50)   String title, 
    @NotNull                    Severity severity,
    @NotBlank @Size(max = 250)  String description, 
    @NotNull @Positive          Integer slaMinutes,
    /**/                        Long assigneeId)
    {
}
