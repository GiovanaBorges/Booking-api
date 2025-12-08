package com.booking.booking.DTO;

import com.booking.booking.ENUMS.RolesENUM;

public record UserResponseDTO(
    Long id,
    String email,
    String password,
    RolesENUM roles
){}