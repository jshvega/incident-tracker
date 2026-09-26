package com.manfred.incidenttracker.dto;

import java.time.OffsetDateTime;

public record CommentResponse(
    Long id,
    String body,
    Long authorId,
    String authorEmail,
    OffsetDateTime createdAt
){
}
