package com.booking.booking.DTO;

import com.booking.booking.ENUMS.RolesENUM;

public record UserRequestDTO(
    String email,
    String password,
    RolesENUM roles
) {}
