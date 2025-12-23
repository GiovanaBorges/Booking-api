package com.booking.booking.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.services.UsersServices;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/users")
public class MeController {
    
    @Autowired
    private UsersServices usersServices;

    @GetMapping("/me")
    public UserResponseDTO me(JwtAuthenticationToken auth) {
        return usersServices.createOrGet(auth.getToken());
    }
    
    
}
