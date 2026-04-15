package com.booking.booking.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.AuthenticatedUserService;
import com.booking.booking.services.ProviderAvailabilityservices;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProviderAvailabilityIntegrationTest extends IntegrationTestBase{

    @Autowired
    private ProviderAvailabilityservices service;

    @Autowired
    private ProviderAvailabilityRepository repository;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private MessageProducerProvider messageProducerProvider;

    @MockBean
    private AuthenticatedUserService authUserService;

    @MockBean
    private UserResolver userResolver;

    private Users provider;

    @BeforeEach
    void setup() {

        provider = usersRepository.save(
                Users.builder()
                        .id(2L)
                        .email("provider@email.com")
                        .name("provider")
                        .keycloakId("kc-provider-123")
                        .roles(RolesENUM.PROVIDER)
                        .build());

        when(authUserService.getAuthenticatedUser())
                .thenReturn(provider);

        when(userResolver.getAuthenticatedUser())
                .thenReturn(provider);
    }

    @Test
    void shouldCreateProviderIntegration() {

        ProviderAvailabilityRequestDTO request = new ProviderAvailabilityRequestDTO(
                5,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0));

        when(userResolver.getAuthenticatedUser())
                .thenReturn(provider);

        // EXECUTE (fluxo real)
        ProviderAvailabilityResponseDTO response = service.saveProviderAvailability(request);

        // ASSERT
        assertNotNull(response.id());
        assertEquals(5, response.dayOfWeek());

        // valida banco REAL
        List<ProviderAvailability> list = repository.findAll();
        assertEquals(1, list.size());

        ProviderAvailability saved = list.get(0);

        assertEquals(provider.getId(), saved.getProvider().getId());
        assertEquals(5, saved.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), saved.getStartTime());

        // evento disparado
        verify(messageProducerProvider).sendEvent(any());
    }

    @Test
    void shouldFindProviderByIdIntegration() {

        ProviderAvailability availability = repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(5)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .build());

        // EXECUTE
        ProviderAvailabilityResponseDTO response = service.findProviderById(availability.getId());

        // ASSERT
        assertAll(
                () -> assertEquals(availability.getId(), response.id()),
                () -> assertEquals(5, response.dayOfWeek()));
    }

    @Test
    void shouldReturnErrorOnGetProviderByIdIntegration() {

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.findProviderById(999L);
        });

        assertAll(
                () -> assertEquals("PROVIDER AVAILABILITY NOT FOUND", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));
    }

    @Test
    void shouldDeleteProviderAvailabilityIntegration() {

        when(userResolver.getAuthenticatedUser()).thenReturn(provider);

        ProviderAvailability availability = repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(5)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .build());

        // EXECUTE
        service.deleteProviderById(availability.getId());

        // ASSERT
        Optional<ProviderAvailability> deleted = repository.findById(availability.getId());

        assertTrue(deleted.isEmpty());

        // evento
        verify(messageProducerProvider).sendEvent(any());
    }

    @Test
    void shouldReturnForbiddenWhenDeletingOtherUserProvider() {

        Users anotherUser = usersRepository.save(
                Users.builder()
                        .email("other@email.com")
                        .name("other")
                        .roles(RolesENUM.PROVIDER)
                        .keycloakId("kc-provider-124")
                        .build());

        when(userResolver.getAuthenticatedUser()).thenReturn(anotherUser);

        ProviderAvailability availability = repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(5)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .build());

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.deleteProviderById(availability.getId());
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

        verify(messageProducerProvider, never()).sendEvent(any());
    }

    @Test
    void shouldReturnErrorOnDeleteProviderIntegration() {

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.deleteProviderById(999L);
        });

        assertAll(
                () -> assertEquals("PROVIDER AVAILABILITY NOT FOUND", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));

        verify(messageProducerProvider, never()).sendEvent(any());
    }

    @Test
    void shouldUpdateProviderAvailabilitySuccessfullyIntegration() {

        ProviderAvailability availability = repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(5)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(18, 0))
                        .build());

        ProviderAvailabilityRequestDTO request = new ProviderAvailabilityRequestDTO(
                6,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0));

        // EXECUTE
        ProviderAvailabilityResponseDTO response = service.updateProvider(availability.getId(), request);

        // ASSERT
        assertEquals(availability.getId(), response.id());
        assertEquals(6, response.dayOfWeek());

        // valida no banco REAL
        ProviderAvailability updated = repository.findById(availability.getId()).orElseThrow();

        assertEquals(6, updated.getDayOfWeek());
        assertEquals(LocalTime.of(14, 0), updated.getStartTime());

        // evento
        verify(messageProducerProvider).sendEvent(any());
    }

    @Test
    void shouldGetAllProviderIntegration() {

        repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(4)
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now())
                        .build());

        repository.save(
                ProviderAvailability.builder()
                        .provider(provider)
                        .dayOfWeek(6)
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now())
                        .build());

        // EXECUTE
        List<ProviderAvailabilityResponseDTO> response = service.getAllProvider();

        // ASSERT
        assertEquals(2, response.size());
    }

    @Test
    void shouldReturnErrorOnGetAllProvidersIntegration() {

        ApiException exception = assertThrows(ApiException.class,
                () -> service.getAllProvider());

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
