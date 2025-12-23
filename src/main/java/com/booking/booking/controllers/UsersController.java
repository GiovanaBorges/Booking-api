package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.UserRequestDTO;
import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.services.UsersServices;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/users")
public class UsersController {
    @Autowired
    private UsersServices service;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> RegisterUser(
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader(value = "Idempotency-Key", required = false) String key) {

        return ResponseEntity.ok().body(service.getOrCreateUser(jwt,key));
    }

   
    
}
