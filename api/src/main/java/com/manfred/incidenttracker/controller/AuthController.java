package com.manfred.incidenttracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manfred.incidenttracker.dto.LoginRequest;
import com.manfred.incidenttracker.dto.LoginResponse;
import com.manfred.incidenttracker.dto.RegisterRequest;
import com.manfred.incidenttracker.dto.RegisterResponse;
import com.manfred.incidenttracker.service.AuthService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req){
        RegisterResponse res = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req){
        LoginResponse res = authService.login(req);
        return ResponseEntity.ok(res);
    }

}