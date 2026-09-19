package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.Escalation;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    
}
