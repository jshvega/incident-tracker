package com.manfred.incidenttracker.events;

import java.time.Instant;

public record IncidentCreatedDetail(
    int schemaVersion,
    Long incidentId,
    String title,
    String severity,
    Integer slaMinutes,
    Instant dueAt,
    Long reporterId,
    Instant createdAt
) {}