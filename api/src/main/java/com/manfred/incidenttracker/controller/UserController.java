package com.manfred.incidenttracker.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manfred.incidenttracker.dto.ChangeRoleRequest;
import com.manfred.incidenttracker.dto.RegisterResponse;
import com.manfred.incidenttracker.service.UserService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PatchMapping("/{id}/role")
    public RegisterResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest req){
        RegisterResponse res = userService.changeRole(id, req);
        return res;
    }

}
