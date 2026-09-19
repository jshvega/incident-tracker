package com.manfred.incidenttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manfred.incidenttracker.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    
}
