package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.services.UsersServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {
    @Autowired
    private UsersServices service;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> RegisterUser(
        @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok().body(service.createOrGet(jwt));
    }

   
    
}
