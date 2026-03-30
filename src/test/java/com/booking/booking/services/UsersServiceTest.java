package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.booking.booking.exceptions.ApiException;
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
        private KeycloakService keycloakService;

        @Mock
        private UserMapper mapperUser;

        @Mock
        private UserEventMapper userEventMapper;

        @Mock
        private UserResolver userResolver;

        @Mock
        private UserFallbackFactory userFallbackFactory;

        @Test
        void shouldReturnExistingUser() {

                Jwt jwt = mock(Jwt.class);

                when(jwt.getSubject()).thenReturn("keycloakId123");
                when(jwt.getClaim("email")).thenReturn("email@email.com");
                when(jwt.getClaim("preferred_username")).thenReturn("user1");
                when(jwt.getClaim("account_type")).thenReturn("provider"); 

                Users existingUser = Users.builder()
                                .id(1L)
                                .keycloakId("keycloakId123")
                                .email("email@email.com")
                                .name("user1")
                                .skills(Set.of(TechSkillsENUM.ANGULAR, TechSkillsENUM.AI))
                                .roles(RolesENUM.PROVIDER)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(repository.findByKeycloakId("keycloakId123"))
                                .thenReturn(Optional.of(existingUser));

                Users response = service.createOrGet(jwt);

                assertEquals(RolesENUM.PROVIDER, response.getRoles());
                assertEquals("keycloakId123", response.getKeycloakId());

                verify(repository, never()).save(any());
                verify(messageProducerUsers, never()).sendEvent(any());

        }

        @Test
        void shouldCreateUserWhenNotExists() {

                Jwt jwt = mock(Jwt.class);

                when(jwt.getSubject()).thenReturn("keycloakId123");
                when(jwt.getClaim("email")).thenReturn("email@email.com");
                when(jwt.getClaim("preferred_username")).thenReturn("user1");
                when(jwt.getClaim("account_type")).thenReturn("provider"); 

                when(repository.findByKeycloakId("keycloakId123"))
                                .thenReturn(Optional.empty());

                when(repository.findByEmail("email@email.com"))
                                .thenReturn(Optional.empty());

                when(repository.save(any(Users.class)))
                                .thenAnswer(invocation -> {
                                        Users u = invocation.getArgument(0);
                                        u.setId(1L);
                                        return u;
                                });


                Users response = service.createOrGet(jwt);

                assertEquals(RolesENUM.PROVIDER, response.getRoles());

                verify(repository).save(any());
                verify(messageProducerUsers,never()).sendEvent(any());

                verify(keycloakService).assignGroup("keycloakId123", "providers");
                verify(keycloakService).assignRoleToUser("keycloakId123", "PROVIDER");
        }

        // ===============================
        // Test editUser
        // ===============================
        @Test
        void shouldEditOwnUserSuccessfully() {

                Jwt jwt = mock(Jwt.class);
                when(jwt.getSubject()).thenReturn("kc-123");

                Users existingUser = Users.builder()
                                .id(1L)
                                .keycloakId("kc-123")
                                .name("Old Name")
                                .email("old@email.com")
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
                                Set.of(TechSkillsENUM.ANGULAR));

                when(repository.findByKeycloakId("kc-123"))
                                .thenReturn(Optional.of(existingUser));

                when(repository.save(any()))
                                .thenAnswer(inv -> inv.getArgument(0));

                doAnswer(inv -> {
                        UserRequestDTO d = inv.getArgument(0);
                        Users u = inv.getArgument(1);

                        u.setName(d.name());
                        u.setDescription(d.description());
                        u.setLinkedinProfile(d.linkedinProfile());
                        u.setGithubProfile(d.githubProfile());
                        u.setExperienceYears(d.experienceYears());
                        u.setSkills(d.skills());

                        return null;
                }).when(mapperUser).updateEntity(any(), any());

                when(mapperUser.toResponse(any()))
                                .thenAnswer(inv -> {
                                        Users u = inv.getArgument(0);
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
                                                        u.getCreatedAt());
                                });

                UserResponseDTO response = service.editUser(dto, jwt);

                assertEquals("New Name", response.name());

                verify(repository, times(1)).findByKeycloakId("kc-123");
                verify(repository, times(1)).save(existingUser);
                verify(messageProducerUsers, times(1))
                                .sendEvent(any());
        }

        @Test
        void shouldThrowWhenUserNotFound() {

                Jwt jwt = mock(Jwt.class);
                when(jwt.getSubject()).thenReturn("kc-999");

                when(repository.findByKeycloakId("kc-999"))
                                .thenReturn(Optional.empty());

                UserRequestDTO dto = new UserRequestDTO(
                                "Name",
                                "email",
                                "desc",
                                "l",
                                "g",
                                1,
                                Set.of());

                assertThrows(ApiException.class, () -> {
                        service.editUser(dto, jwt);
                });

                verify(repository, never()).save(any());
                verify(messageProducerUsers, never()).sendEvent(any());
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
                                                existingUser.getCreatedAt()));
                UserResponseDTO response = service.getUserById(1L);

                assertAll(
                                () -> assertEquals(1L, response.id()),
                                () -> assertEquals("User One", response.name()),
                                () -> assertEquals(2, response.skills().size()));

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
                                () -> assertEquals(0, fallback.skills().size()));
        }

        @Test
        void shouldReturnAllProvidersSuccessfully() {

                Users provider1 = Users.builder()
                                .id(1L)
                                .name("Provider One")
                                .email("p1@email.com")
                                .roles(RolesENUM.PROVIDER)
                                .createdAt(LocalDateTime.now())
                                .build();

                Users provider2 = Users.builder()
                                .id(2L)
                                .name("Provider Two")
                                .email("p2@email.com")
                                .roles(RolesENUM.PROVIDER)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(repository.findByRoles(RolesENUM.PROVIDER))
                                .thenReturn(List.of(provider1, provider2));

                when(mapperUser.toResponse(any()))
                                .thenAnswer(inv -> {
                                        Users u = inv.getArgument(0);
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
                                                        u.getCreatedAt());
                                });

                List<UserResponseDTO> response = service.getAllProviders();

                assertEquals(2, response.size());

                verify(repository)
                                .findByRoles(RolesENUM.PROVIDER);

                verify(mapperUser, times(2))
                                .toResponse(any());
        }

        @Test
        void shouldThrowWhenNoProvidersFound() {

                when(repository.findByRoles(RolesENUM.PROVIDER))
                                .thenReturn(Collections.emptyList());

                ApiException exception = assertThrows(ApiException.class, () -> {
                        service.getAllProviders();
                });

                assertEquals("NO PROVIDERS FOUND", exception.getMessage());

                verify(repository)
                                .findByRoles(RolesENUM.PROVIDER);

                verify(mapperUser, never())
                                .toResponse(any());
        }

}
