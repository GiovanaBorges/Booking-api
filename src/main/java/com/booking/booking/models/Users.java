package com.booking.booking.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.antlr.v4.runtime.misc.NotNull;

import com.booking.booking.ENUMS.RolesEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Email
    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private RolesEnum roles;
    
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
