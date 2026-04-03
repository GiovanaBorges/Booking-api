package com.booking.booking.DTO.requests;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.validator.constraints.URL;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Enumerated;

public record UserRequestDTO(
    String name,
    String description,
    String linkedinProfile,
    String githubProfile,
    String portfolioUrl,
    Integer experienceYears,
    Set<TechSkillsENUM> skills
){}
