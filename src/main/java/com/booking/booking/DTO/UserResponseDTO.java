package com.booking.booking.DTO;

import java.time.LocalDateTime;

import com.booking.booking.ENUMS.RolesENUM;

public record UserResponseDTO(
    Long id,
    String name,
    String email,
    RolesENUM roles,
    LocalDateTime createdAt
){}