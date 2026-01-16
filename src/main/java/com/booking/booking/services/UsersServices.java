package com.booking.booking.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

@Service
public class UsersServices {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MessageProducerUsers messageProducerUsers;

    @Autowired
    private LockService idempotencyService;

    public UserResponseDTO getOrCreateUser(Jwt jwt, String idempotencyKey) {

        return idempotencyService.execute(
                idempotencyKey,
                () -> createOrGet(jwt));
    }

    public UserResponseDTO createOrGet(Jwt jwt) {

        String keycloakId = jwt.getSubject();
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");

        RolesENUM role = RolesENUM.ADMIN;
        Users user = usersRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {

                    Users newUser = Users.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .name(name)
                            .roles(role)
                            .createdAt(LocalDateTime.now())
                            .build();

                    Users savedUser = usersRepository.save(newUser);

                    messageProducerUsers.sendUsersCreateEvent(
                            UsersCreatedEvent.builder()
                                    .id(savedUser.getId())
                                    .name(savedUser.getName())
                                    .email(savedUser.getEmail())
                                    .roles(savedUser.getRoles().toString())
                                    .createdAt(LocalDateTime.now())
                                    .build());

                    return savedUser;
                });

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt());
    }

}
