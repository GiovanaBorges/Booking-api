package com.booking.booking.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(unique = true, nullable = false)
    private String keycloakId;

    @Email
    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private RolesENUM roles;

    @Column(nullable = true)
    private String Description;

    @URL(message = "Linkedin inválido")
    @Column(nullable = true)
    private String linkedinProfile;

    @URL(message = "Github inválido")
    @Column(nullable = true)
    private String githubProfile;

    @URL(message = "Portfólio inválido")
    @Column(nullable = true)
    private String portfolioUrl;

    @Column(nullable = true)
    private Integer experienceYears;

    @ElementCollection(targetClass = TechSkillsENUM.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "skills")
    private Set<TechSkillsENUM> skills = new HashSet<>();
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
