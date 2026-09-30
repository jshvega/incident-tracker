package com.manfred.incidenttracker.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.dto.RegisterRequest;
import com.manfred.incidenttracker.dto.RegisterResponse;
import com.manfred.incidenttracker.entity.Role;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.EmailAlreadyExistsException;
import com.manfred.incidenttracker.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service 
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional 
    public RegisterResponse register(RegisterRequest req){

        String email = req.email().trim().toLowerCase();
        if(userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException(email);

        String hash = passwordEncoder.encode(req.password());

        User user = new User(email, hash, Role.reporter, null);

        User saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getUserRole().name());

    }
    
}
