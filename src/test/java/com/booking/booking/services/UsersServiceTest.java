package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.mappers.UserMapper;
import com.booking.booking.mappers.events.UserEventMapper;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.helpers.UserFallbackFactory;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    @Mock
    private UsersRepository repository;

    @Mock
    private MessageProducerUsers messageProducerUsers;

    @InjectMocks
    private UsersServices service;

    @Mock
    private UserMapper mapperUser;

    @Mock
    private UserEventMapper userEventMapper;

    @Mock 
    private UserResolver userResolver;

    @Mock
    private UserFallbackFactory userFallbackFactory;

    @Test
    void shouldReturnExistingUser(){

        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn("keycloakId123");
        when(jwt.getClaim("email")).thenReturn("email@email.com");
        when(jwt.getClaim("preferred_username")).thenReturn("user1");
        
        Users existingUser = Users.builder()
            .id(1L)
            .keycloakId("keycloakId123")
            .email("email@email.com")
            .name("user1")
            .skills(Set.of(TechSkillsENUM.ANGULAR,TechSkillsENUM.AI))
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        UserResponseDTO dto =
                new UserResponseDTO(1L, "user1", "email@email.com", RolesENUM.PROVIDER, 
                "Description", 
                "http://linkedin.com/in/user1", 
                "http://github.com/user1", 
                "http://twitter.com/user1", 
                2, 
                 existingUser.getSkills(), 
                existingUser.getCreatedAt());

        when(repository.findByKeycloakId("keycloakId123"))
            .thenReturn(Optional.of(existingUser));

        when(repository.findByKeycloakId("keycloakId123"))
            .thenReturn(Optional.of(existingUser));

        when(mapperUser.toResponse(existingUser))
                .thenReturn(dto);

        UserResponseDTO responseDTO = service.createOrGet(jwt);

        assertAll(
            () -> assertEquals(existingUser.getId(), responseDTO.id()),
            () -> assertEquals(existingUser.getName(), responseDTO.name()),
            () -> assertEquals(existingUser.getEmail(), responseDTO.email()),
            () -> assertEquals(existingUser.getRoles(), responseDTO.roles()),
            () -> assertEquals(RolesENUM.PROVIDER, responseDTO.roles()),
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

        when(repository.findByKeycloakId("keycloakId123"))
            .thenReturn(Optional.empty());

        when(repository.save(any(Users.class)))
            .thenAnswer(invocation -> {
                Users u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });

        UsersCreatedEvent event = UsersCreatedEvent.builder()
            .id(1L)
            .name("user1")
            .roles(RolesENUM.ADMIN.toString())
            .createdAt(LocalDateTime.now())
            .build();
            

        when(userEventMapper.toCreateEvent(any()))
                .thenReturn(event);

        UserResponseDTO dto =
                new UserResponseDTO(
                    1L, 
                    "user1",
                 "email@email.com", 
                RolesENUM.ADMIN, 
                "Description", 
                "http://linkedin.com/in/user1", 
                "http://github.com/user1", 
                "http://twitter.com/user1", 
                2, 
                Set.of(TechSkillsENUM.ANGULAR,TechSkillsENUM.AI),
                LocalDateTime.now());

        when(mapperUser.toResponse(any()))
                .thenReturn(dto);

        UserResponseDTO responseDTO = service.createOrGet(jwt);

        assertAll(
            () -> assertEquals(1L, responseDTO.id()),
            () -> assertEquals("user1", responseDTO.name()),
            () -> assertEquals("email@email.com", responseDTO.email()),
            () -> assertEquals(RolesENUM.ADMIN, responseDTO.roles()),
            () -> assertEquals(responseDTO.createdAt(), responseDTO.createdAt())
        );

        verify(repository, times(1)).findByKeycloakId("keycloakId123");
        verify(repository, times(1)).save(any(Users.class));
        verify(messageProducerUsers, times(1))
            .sendUsersCreateEvent(any(UsersCreatedEvent.class));
    }

    // ===============================
    // Test editUser
    // ===============================
    @Test
    void shouldEditExistingUser() {
        Users existingUser = Users.builder()
                .id(1L)
                .name("User One")
                .email("user@email.com")
                .Description("Old desc")
                .skills(new HashSet<>(Set.of(TechSkillsENUM.JAVA, TechSkillsENUM.REACT)))
                .roles(RolesENUM.PROVIDER)
                .createdAt(LocalDateTime.now())
                .build();

        UserRequestDTO dto = new UserRequestDTO(
                "New Name",              
                "new@email.com",          
                "New desc",               
                "linkedin.com/new",        
                "github.com/new",       
                5,                        
                Set.of(TechSkillsENUM.ANGULAR, TechSkillsENUM.AI) 
        );

        when(userResolver.resolveUserById(1L)).thenReturn(existingUser);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(mapperUser.toResponse(any())).thenAnswer(invocation -> {
            Users u = invocation.getArgument(0);
            return new UserResponseDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRoles(),
                u.getDescription(),
                u.getLinkedinProfile(),
                u.getGithubProfile(),
                u.getPortfolioUrl(),
                u.getExperienceYears(),
                u.getSkills(),
                u.getCreatedAt()
            );
        });

        // Chama o service
        UserResponseDTO response = service.editUser(1L, dto);

        assertEquals(1L, response.id());

        verify(repository, times(1)).save(existingUser);
        verify(userEventMapper, times(1)).toUpdatedEvent(existingUser);
    }

    // ===============================
    // Test getUserById com fallback
    // ===============================
    @Test
    void shouldReturnUserById() {
        Users existingUser = Users.builder()
                .id(1L)
                .name("User One")
                .email("user@email.com")
                .skills(new HashSet<>(Set.of(TechSkillsENUM.JAVA, TechSkillsENUM.REACT)))
                .roles(RolesENUM.PROVIDER)
                .createdAt(LocalDateTime.now())
                .build();

            when(userResolver.resolveUserById(1L)).thenReturn(existingUser);

            when(mapperUser.toResponse(existingUser)).thenReturn(
                    new UserResponseDTO(
                        existingUser.getId(),
                        existingUser.getName(),
                        existingUser.getDescription(),
                        existingUser.getRoles(), 
                        existingUser.getLinkedinProfile(),
                        existingUser.getGithubProfile(),
                        existingUser.getPortfolioUrl(),
                        existingUser.getPortfolioUrl(),
                        existingUser.getExperienceYears(), 
                        existingUser.getSkills(), 
                        existingUser.getCreatedAt()
                    )
            );
        UserResponseDTO response = service.getUserById(1L);

        assertAll(
                () -> assertEquals(1L, response.id()),
                () -> assertEquals("User One", response.name()),
                () -> assertEquals(2, response.skills().size())
        );

        verify(userResolver, times(1)).resolveUserById(1L);
        verify(mapperUser, times(1)).toResponse(existingUser);
    }

     @Test
    void shouldReturnFallbackUserWhenGetUserByIdFails() {
        lenient().when(userResolver.resolveUserById(2L)).thenThrow(new RuntimeException("DB down"));
        lenient().when(userFallbackFactory.createFallback(2L)).thenCallRealMethod();

        UserResponseDTO fallback = service.handleUserEventFailure(2L, new RuntimeException());

        assertAll(
            () -> assertEquals(2L, fallback.id()),
            () -> assertEquals("USER SERVICE TEMPORARILY UNAVAILABLE", fallback.name()),
            () -> assertEquals(0, fallback.skills().size())
        );
    }

}
