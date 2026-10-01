package com.manfred.incidenttracker.security;

import com.manfred.incidenttracker.entity.Role;

public record AuthUser(Long id, Role role) {
}
