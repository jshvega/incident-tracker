package com.manfred.incidenttracker.dto;

import com.manfred.incidenttracker.entity.Status;

import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull Status status) {
}
