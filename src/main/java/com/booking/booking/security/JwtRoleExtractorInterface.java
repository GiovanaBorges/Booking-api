package com.booking.booking.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.booking.booking.ENUMS.RolesENUM;

public interface JwtRoleExtractorInterface {
    RolesENUM extractRole(Jwt jwt);
}
