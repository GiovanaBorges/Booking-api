package com.booking.booking.services.helpers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.TechSkillsENUM;

@Component
public class UserFallbackFactory {
     // fallback para getUserById, editUser etc.
    public UserResponseDTO createFallback(Long id) {
        return new UserResponseDTO(
                id,
                "USER SERVICE TEMPORARILY UNAVAILABLE", // name
                null, // description
                null, // roles
                null, // linkedinProfile
                null, // githubProfile
                null, // portfolioUrl
                null, // duplicated portfolioUrl? se seu construtor tem
                0, // experienceYears
                new HashSet<>(), // skills
                null // createdAt
        );
    }

    // fallback para Jwt-based methods (ex: createOrGet)
    public UserResponseDTO createFallbackFromJwt(String keycloakId) {
        return new UserResponseDTO(
                -1L,
                "USER SERVICE TEMPORARILY UNAVAILABLE",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                new HashSet<>(),
                null
        );
    }

    // fallback para Skill-based lists
    public List<UserResponseDTO> createFallbackForSkill(TechSkillsENUM skill) {
        return Collections.emptyList(); // Retorna lista vazia
    }

    // fallback para editUser com DTO (pode usar o mesmo createFallback(id))
    public UserResponseDTO createFallbackForEdit(Long id) {
        return createFallback(id);
    }
}
