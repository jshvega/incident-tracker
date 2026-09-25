package com.manfred.incidenttracker.dto;

import java.time.OffsetDateTime;

public record StatusHistoryEntry(
    String fromStatus, 
    String toStatus, 
    Long changedByUserId, 
    String userEmail,
    OffsetDateTime changedAt) {
}
