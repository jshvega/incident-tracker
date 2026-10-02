package com.manfred.incidenttracker.dto;

import com.manfred.incidenttracker.entity.Role;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
    @NotNull Role role
) {
}
