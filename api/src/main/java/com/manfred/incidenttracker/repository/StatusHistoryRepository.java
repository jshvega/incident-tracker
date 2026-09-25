package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.StatusHistory;
import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    public List<StatusHistory> findByIncidentIdOrderByChangedAtAsc(Long incidentId);

}