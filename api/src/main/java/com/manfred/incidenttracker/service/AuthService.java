package com.manfred.incidenttracker.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.manfred.incidenttracker.dto.LoginRequest;
import com.manfred.incidenttracker.dto.LoginResponse;
import com.manfred.incidenttracker.dto.RegisterRequest;
import com.manfred.incidenttracker.dto.RegisterResponse;
import com.manfred.incidenttracker.entity.Role;
import com.manfred.incidenttracker.entity.User;
import com.manfred.incidenttracker.exception.EmailAlreadyExistsException;
import com.manfred.incidenttracker.exception.InvalidCredentialsException;
import com.manfred.incidenttracker.repository.UserRepository;
import com.manfred.incidenttracker.security.JwtService;

import jakarta.transaction.Transactional;

@Service 
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    public LoginResponse login(LoginRequest req){

        //Normilize email
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(req.password(), user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }

        return jwtService.issue(user);

    }
    
}
