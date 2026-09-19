package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    
}
