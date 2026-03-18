package com.booking.booking.DTO.responses;

import java.time.LocalDateTime;
import java.util.Set;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;

public record UserResponseDTO(
    Long id,
    String name,
    String email,
    RolesENUM roles,
    String Description,
    String linkedinProfile,
    String githubProfile,
    String portfolioUrl,
    Integer experienceYears,
    Set<TechSkillsENUM> skills,
    LocalDateTime createdAt
){}