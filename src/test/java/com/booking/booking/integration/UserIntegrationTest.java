package com.booking.booking.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.KeycloakService;
import com.booking.booking.services.UsersServices;
import com.booking.booking.services.helpers.UserFallbackFactory;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserIntegrationTest {

    @Autowired
    private UsersServices service;

    @Autowired
    private UsersRepository repository;

    @MockBean
    private MessageProducerUsers messageProducerUsers;

    @MockBean
    private KeycloakService keycloakService;

    @MockBean
    private UserResolver userResolver;

    @MockBean
    private UserFallbackFactory userFallbackFactory;

    private Users existingUser;

    @Test
    void shouldReturnExistingUserIntegration() {

        Users existingUser = repository.save(
                Users.builder()
                        .keycloakId("keycloakId123")
                        .email("email@email.com")
                        .name("user1")
                        .roles(RolesENUM.PROVIDER)
                        .skills(new HashSet<>(Set.of(
                                TechSkillsENUM.JAVA,
                                TechSkillsENUM.REACT)))
                        .build());

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("keycloakId123");
        when(jwt.getClaim("email")).thenReturn("email@email.com");
        when(jwt.getClaim("preferred_username")).thenReturn("user1");
        when(jwt.getClaim("account_type")).thenReturn("provider");

        // EXECUTE
        Users response = service.createOrGet(jwt);

        // ASSERT
        assertEquals(existingUser.getId(), response.getId());
        assertEquals(RolesENUM.PROVIDER, response.getRoles());

        // banco não cria outro
        List<Users> all = repository.findAll();
        assertEquals(1, all.size());

        // nada externo chamado
        verify(keycloakService).assignGroup("keycloakId123", "providers");
        verify(keycloakService).assignRoleToUser("keycloakId123", "PROVIDER");

        verify(messageProducerUsers, never()).sendEvent(any());
    }

    @Test
    void shouldCreateUserWhenNotExistsIntegration() {

        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn("keycloakId123");
        when(jwt.getClaim("email")).thenReturn("email@email.com");
        when(jwt.getClaim("preferred_username")).thenReturn("user1");
        when(jwt.getClaim("account_type")).thenReturn("provider");

        // EXECUTE
        Users response = service.createOrGet(jwt);

        // ASSERT
        assertNotNull(response.getId());
        assertEquals(RolesENUM.PROVIDER, response.getRoles());

        // valida banco REAL
        Users saved = repository.findByKeycloakId("keycloakId123")
                .orElseThrow();

        assertEquals("user1", saved.getName());
        assertEquals("email@email.com", saved.getEmail());

        // integrações externas
        verify(keycloakService).assignGroup("keycloakId123", "providers");
        verify(keycloakService).assignRoleToUser("keycloakId123", "PROVIDER");

        verify(messageProducerUsers, never()).sendEvent(any());
    }

    @Test
    void shouldEditOwnUserSuccessfullyIntegration() {

         Users user = repository.save(
                Users.builder()
                        .id(1L)
                        .name("User One")
                        .keycloakId("keycloak-123")
                        .skills(new HashSet<>(Set.of(
                                TechSkillsENUM.JAVA,
                                TechSkillsENUM.REACT)))
                        .roles(RolesENUM.PROVIDER)
                        .experienceYears(3)
                        .createdAt(LocalDateTime.now())
                        .build());

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("keycloak-123");

        UserRequestDTO dto = new UserRequestDTO(
                "New Name",
                "New desc",
                "http://linkedin.com/new",
                "http://github.com/new",
                "http://portfolio.com/new",
                5,
                Set.of(TechSkillsENUM.ANGULAR));

        // EXECUTE
        UserResponseDTO response = service.editUser(dto, jwt);

        // ASSERT RESPONSE
        assertEquals("New Name", response.name());

        // ASSERT BANCO REAL
        Users updated = repository.findByKeycloakId("keycloak-123")
                .orElseThrow();

        assertEquals("New Name", updated.getName());
        assertEquals("New desc", updated.getDescription());
        assertEquals(5, updated.getExperienceYears());
        assertTrue(updated.getSkills().contains(TechSkillsENUM.ANGULAR));

        // evento disparado
        verify(messageProducerUsers).sendEvent(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundIntegration() {

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("kc-999");

        UserRequestDTO dto = new UserRequestDTO(
                "Name",
                "email",
                "desc",
                "l",
                "g",
                1,
                Set.of());

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.editUser(dto, jwt);
        });

        assertNotNull(exception);

        // banco continua vazio
        assertTrue(repository.findAll().isEmpty());

        verify(messageProducerUsers, never()).sendEvent(any());
    }

    @Test
    void shouldReturnUserByIdIntegration() {

        Users user = repository.save(
                Users.builder()
                        .id(1L)
                        .name("User One")
                        .email("user@email.com")
                        .keycloakId("keycloak-123")
                        .skills(new HashSet<>(Set.of(
                                TechSkillsENUM.JAVA,
                                TechSkillsENUM.REACT)))
                        .roles(RolesENUM.PROVIDER)
                        .createdAt(LocalDateTime.now())
                        .build());

        when(userResolver.resolveUserById(user.getId()))
                .thenReturn(user);

        // EXECUTE
        UserResponseDTO response = service.getUserById(user.getId());

        // ASSERT
        assertAll(
                () -> assertEquals(user.getId(), response.id()),
                () -> assertEquals("User One", response.name()),
                () -> assertEquals(2, response.skills().size()));
    }

    @Test
    void shouldReturnFallbackUserWhenGetUserByIdFailsIntegration() {

        when(userResolver.resolveUserById(2L))
                .thenThrow(new RuntimeException("DB down"));

        when(userFallbackFactory.createFallback(2L))
                .thenCallRealMethod();

        UserResponseDTO fallback = service.handleUserEventFailure(2L, new RuntimeException());

        assertAll(
                () -> assertEquals(2L, fallback.id()),
                () -> assertEquals("USER SERVICE TEMPORARILY UNAVAILABLE", fallback.name()),
                () -> assertEquals(0, fallback.skills().size()));
    }

    @Test
    void shouldReturnAllProvidersSuccessfullyIntegration() {

        // ARRANGE
        Users provider1 = repository.save(
                Users.builder()
                        .name("Provider One")
                        .email("p1@email.com")
                        .keycloakId("kc-1")
                        .roles(RolesENUM.PROVIDER)
                        .createdAt(LocalDateTime.now())
                        .build());

        Users provider2 = repository.save(
                Users.builder()
                        .name("Provider Two")
                        .email("p2@email.com")
                        .keycloakId("kc-2")
                        .roles(RolesENUM.PROVIDER)
                        .createdAt(LocalDateTime.now())
                        .build());

        // EXECUTE
        List<UserResponseDTO> response = service.getAllProviders();

        // ASSERT
        assertEquals(2, response.size());

        // valida conteudo do response
        assertTrue(
                response.stream().anyMatch(u -> u.name().equals("Provider One")));

        assertTrue(
                response.stream().anyMatch(u -> u.name().equals("Provider Two")));
    }

    @Test
    void shouldThrowWhenNoProvidersFoundIntegration() {

        // banco vazio por padrão (ou limpa antes)

        // EXECUTE + ASSERT
        ApiException exception = assertThrows(ApiException.class, () -> {
            service.getAllProviders();
        });

        assertEquals("NO PROVIDERS FOUND", exception.getMessage());
    }

    @Test
    void shouldReturnUsersBySkillIntegration() {

        // ARRANGE
        Users user1 = repository.save(
                Users.builder()
                        .name("User Java 1")
                        .email("java1@email.com")
                        .keycloakId("kc-1")
                        .roles(RolesENUM.PROVIDER)
                        .skills(Set.of(TechSkillsENUM.JAVA, TechSkillsENUM.REACT))
                        .createdAt(LocalDateTime.now())
                        .build());

        Users user2 = repository.save(
                Users.builder()
                        .name("User Java 2")
                        .email("java2@email.com")
                        .keycloakId("kc-2")
                        .roles(RolesENUM.PROVIDER)
                        .skills(Set.of(TechSkillsENUM.JAVA))
                        .createdAt(LocalDateTime.now())
                        .build());

        Users user3 = repository.save(
                Users.builder()
                        .name("User Angular")
                        .email("angular@email.com")
                        .keycloakId("kc-3")
                        .roles(RolesENUM.PROVIDER)
                        .skills(Set.of(TechSkillsENUM.ANGULAR))
                        .createdAt(LocalDateTime.now())
                        .build());

        // EXECUTE
        List<UserResponseDTO> response = service.getUsersBySkill(TechSkillsENUM.JAVA);

        // ASSERT
        assertEquals(2, response.size());

        // valida se só veio JAVA
        assertTrue(
                response.stream()
                        .allMatch(u -> u.skills().contains(TechSkillsENUM.JAVA)));
    }

    @Test
    void shouldReturnEmptyListWhenNoUsersWithSkillIntegration() {

        // ARRANGE
        // garante que NÃO existe nenhum user com JAVA
        repository.save(
                Users.builder()
                        .name("User Angular")
                        .email("angular@email.com")
                        .keycloakId("kc-1")
                        .roles(RolesENUM.PROVIDER)
                        .skills(Set.of(TechSkillsENUM.ANGULAR))
                        .createdAt(LocalDateTime.now())
                        .build());

        // EXECUTE
        List<UserResponseDTO> response = service.getUsersBySkill(TechSkillsENUM.JAVA);

        // ASSERT
        assertNotNull(response); // nunca deve ser null
        assertTrue(response.isEmpty());
    }

}
