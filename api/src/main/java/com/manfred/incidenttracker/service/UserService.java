package com.manfred.incidenttracker.service;

import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.dto.RegisterResponse;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.UserNotFoundException;
import com.manfred.incidenttracker.repository.UserRepository;
import com.manfred.incidenttracker.dto.ChangeRoleRequest;

import jakarta.transaction.Transactional;

@Service 
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Transactional 
    public RegisterResponse changeRole(Long userId, ChangeRoleRequest req){

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        user.setUserRole(req.role());

        return new RegisterResponse(user.getId(), user.getEmail(), user.getUserRole().name());

    }
    
}
