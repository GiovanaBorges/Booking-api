package com.booking.booking.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.UserRequestDTO;
import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.services.UsersServices;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok().body(service.getUserById(id));
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> RegisterUser(
        @RequestBody UserRequestDTO userRequestDTO,
        @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok().body(service.saveUser(userRequestDTO));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok().body(service.getAllUsers());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@RequestBody UserRequestDTO userRequestDTO,@PathVariable Long id) {
        return ResponseEntity.ok().body(service.updateUser(id, userRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponseDTO> deleteUser(@PathVariable Long id){
        return ResponseEntity.ok().body(service.deleteUser(id));
    }
    
}
