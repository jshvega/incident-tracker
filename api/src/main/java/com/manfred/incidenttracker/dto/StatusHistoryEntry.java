package com.manfred.incidenttracker.dto;

import java.time.OffsetDateTime;

public record StatusHistoryEntry(
    String fromStatus, 
    String toStatus, 
    UserSummary changedByUser,
    OffsetDateTime changedAt) {
}
