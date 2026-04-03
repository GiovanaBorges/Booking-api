package com.booking.booking.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.models.Users;
import com.booking.booking.services.UsersServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/users")
public class MeController {
    
    @Autowired
    private UsersServices usersServices;

    @GetMapping("/me")
    public ResponseEntity<Users> me(JwtAuthenticationToken auth) {
        return ResponseEntity.ok(usersServices.createOrGet(auth.getToken()));
    }
    
    @GetMapping("/skill/{skill}")
    public ResponseEntity<List<UserResponseDTO>> getUsersBySkill(@PathVariable TechSkillsENUM skill) {
        return ResponseEntity.ok(usersServices.getUsersBySkill(skill));
    }

    @PutMapping("/edit")
    public ResponseEntity<UserResponseDTO> editUsers( 
        @RequestBody UserRequestDTO dto,
        JwtAuthenticationToken auth
    ) {
        return ResponseEntity.ok(usersServices.editUser(
            dto, 
            auth.getToken()));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(usersServices.getUserById(id));
    }
    
    @GetMapping("/providers")
    public ResponseEntity<List<UserResponseDTO>> getAllProviders() {
        return ResponseEntity.ok(usersServices.getAllProviders());
    }
}
