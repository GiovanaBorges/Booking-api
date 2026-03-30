package com.booking.booking.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;

@Service
public class AuthenticatedUserService {

    private final UsersServices services;

    public AuthenticatedUserService(UsersServices usersServices){
        this.services = usersServices;
    }
    public Users getAuthenticatedUser() {

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        
        return services.createOrGet(jwt);
    }
}