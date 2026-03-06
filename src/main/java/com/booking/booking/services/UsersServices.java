package com.booking.booking.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.mappers.UserMapper;
import com.booking.booking.mappers.events.UserEventMapper;
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
    private UserMapper mapperUser;

    @Autowired
    private UserEventMapper userEventMapper;

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

                    messageProducerUsers.sendUsersCreateEvent(userEventMapper.toCreateEvent(savedUser));

                    return savedUser;
                });

        return mapperUser.toResponse(user);
    }

}
