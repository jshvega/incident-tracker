package com.manfred.incidenttracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    public List<Comment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
    
}
