package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.security.JwtRoleExtractor;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    @Mock
    private UsersRepository repository;

    @Mock
    private MessageProducerUsers messageProducerUsers;

    @Mock
    private JwtRoleExtractor jwtRoleExtractor;

    @InjectMocks
    private UsersServices service;

    @Test
    void shouldReturnExistingUser(){

        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn("keycloakId123");
        when(jwt.getClaim("email")).thenReturn("email@email.com");
        when(jwt.getClaim("preferred_username")).thenReturn("user1");
        when(jwtRoleExtractor.extractRole(jwt)).thenReturn(RolesENUM.PROVIDER);

        Users existingUser = Users.builder()
            .id(1L)
            .keycloakId("keycloakId123")
            .email("email@email.com")
            .name("user1")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        when(repository.findByKeycloakId("keycloakId123"))
            .thenReturn(Optional.of(existingUser));

        UserResponseDTO responseDTO = service.createOrGet(jwt);

        assertAll(
            () -> assertEquals(existingUser.getId(), responseDTO.id()),
            () -> assertEquals(existingUser.getName(), responseDTO.name()),
            () -> assertEquals(existingUser.getEmail(), responseDTO.email()),
            () -> assertEquals(existingUser.getRoles(), responseDTO.roles()),
            () -> assertEquals(existingUser.getCreatedAt(), responseDTO.createdAt())
        );

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(repository,never()).save(any());
        verify(messageProducerUsers,never()).sendUsersCreateEvent(any());
    }

    @Test
    void shouldCreateUserWhenNotExists() {

        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn("keycloakId123");
        when(jwt.getClaim("email")).thenReturn("email@email.com");
        when(jwt.getClaim("preferred_username")).thenReturn("user1");
        when(jwtRoleExtractor.extractRole(jwt))
            .thenReturn(RolesENUM.PROVIDER);

        when(repository.findByKeycloakId("keycloakId123"))
            .thenReturn(Optional.empty());

        when(repository.save(any(Users.class)))
            .thenAnswer(invocation -> {
                Users u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });

        UserResponseDTO responseDTO = service.createOrGet(jwt);

        assertAll(
            () -> assertEquals(1L, responseDTO.id()),
            () -> assertEquals("user1", responseDTO.name()),
            () -> assertEquals("email@email.com", responseDTO.email()),
            () -> assertEquals(RolesENUM.PROVIDER, responseDTO.roles()),
            () -> assertEquals(responseDTO.createdAt(), responseDTO.createdAt())
        );

        verify(repository, times(1)).findByKeycloakId("keycloakId123");
        verify(repository, times(1)).save(any(Users.class));
        verify(messageProducerUsers, times(1))
            .sendUsersCreateEvent(any(UsersCreatedEvent.class));
    }

}
