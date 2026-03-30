package com.booking.booking.services.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
public class UserResolver {

    @Autowired
    private UsersRepository usersRepository;

    public Users getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }

        String keycloakId = jwt.getSubject(); // 👈 pega o "sub"

        return usersRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() ->
                new ApiException("USER NOT FOUND IN DATABASE", HttpStatus.NOT_FOUND)
            );
    }


    public Users resolveUserById(Long id) {
        return usersRepository.findById(id)
            .orElseThrow(() -> new ApiException("USER NOT FOUND", HttpStatus.NOT_FOUND));

    }
    
   
}