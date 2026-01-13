package com.booking.booking.security;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.exceptions.ApiException;

@Component
public class JwtRoleExtractor implements JwtRoleExtractorInterface {
    
    @Override
    public RolesENUM extractRole(Jwt jwt) {

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null) {
            throw new ApiException("ROLE NOT FOUND", HttpStatus.FORBIDDEN);
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        if (roles.contains("PROVIDER")) return RolesENUM.PROVIDER;
        if (roles.contains("CLIENT")) return RolesENUM.CLIENT;

        throw new ApiException("INVALID ROLE", HttpStatus.FORBIDDEN);
    }
}
