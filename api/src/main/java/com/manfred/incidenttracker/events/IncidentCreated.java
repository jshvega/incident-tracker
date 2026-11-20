package com.manfred.incidenttracker.events;

import java.time.OffsetDateTime;
import com.manfred.incidenttracker.entity.Severity;

public record IncidentCreated(
    Long incidentId,
    String title,
    Severity severity,
    Integer slaMinutes,
    OffsetDateTime dueAt,
    Long reporterId,
    OffsetDateTime createdAt
) {}