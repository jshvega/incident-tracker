package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.Incident;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
