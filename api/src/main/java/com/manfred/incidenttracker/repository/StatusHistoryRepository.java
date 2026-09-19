package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.StatusHistory;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    
}
