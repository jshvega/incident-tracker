package com.manfred.incidenttracker.dto;

public record RegisterResponse(
    Long id,
    String email,
    String role
) {
}
