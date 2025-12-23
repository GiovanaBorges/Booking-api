package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.booking.booking.DTO.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

@ExtendWith(MockitoExtension.class)
public class ProviderAvailabilityServicesTest {

    @Mock
    private ProviderAvailabilityRepository repository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private MessageProducerProvider messageProducerProvider;

    @Mock
    private LockService idempotencyService;

    @InjectMocks
    private ProviderAvailabilityservices service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateProvider() {

        Users user = Users.builder()
                .id(1L)
                .email("email@email.com")
                .name("user1")
                .keycloakId("kc-test-123")
                .roles(RolesENUM.PROVIDER)
                .build();

        ProviderAvailability provider = ProviderAvailability.builder()
                .id(1L)
                .day_of_week(5)
                .start_time(LocalTime.now())
                .end_time(LocalTime.now())
                .provider(user)
                .build();

        ProviderAvailabilityRequestDTO requestDTO = new ProviderAvailabilityRequestDTO(
                provider.getDay_of_week(),
                provider.getStart_time(),
                provider.getEnd_time(),
                user.getId());

        when(usersRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(repository.save(any(ProviderAvailability.class)))
                .thenAnswer(invocation -> {
                    ProviderAvailability p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        // 🔑 Idempotency mock: executa direto a action
        when(idempotencyService.execute(any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        ProviderAvailabilityResponseDTO responseDTO = service.saveProviderAvailability(requestDTO, "idem-key-123");

        assertAll(
                () -> assertEquals(provider.getId(), responseDTO.id()),
                () -> assertEquals(provider.getDay_of_week(), responseDTO.day_of_week()),
                () -> assertEquals(provider.getStart_time(), responseDTO.start_time()),
                () -> assertEquals(provider.getEnd_time(), responseDTO.end_time()),
                () -> assertEquals(provider.getProvider(), responseDTO.provider()));

        verify(usersRepository, times(1)).findById(user.getId());
        verify(repository, times(1)).save(any(ProviderAvailability.class));
        verify(messageProducerProvider, times(1))
                .sendProviderCreateEvent(any(ProviderAvailabilityCreatedEvent.class));
    }

    @Test
    void shouldThrowConflictWhenIdempotencyKeyAlreadyExists() {

        // ===== GIVEN =====
        ProviderAvailabilityRequestDTO requestDTO = new ProviderAvailabilityRequestDTO(
                5,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                1L);

        // Simula Redis dizendo: "essa chave já foi usada"
        when(idempotencyService.execute(any(), any()))
                .thenThrow(new ApiException(
                        "Request already processed",
                        HttpStatus.CONFLICT));

        // ===== WHEN / THEN =====
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.saveProviderAvailability(requestDTO, "idem-key-123"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        // ===== VERIFY: nada aconteceu =====
        verify(usersRepository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(messageProducerProvider, never())
                .sendProviderCreateEvent(any());
    }

    @Test
    void shouldFindProviderById() {
        Users user = Users.builder()
                .id(1L)
                .email("email@email.com")
                .name("user1")
                .keycloakId("kc-test-123")
                .roles(RolesENUM.PROVIDER)
                .build();

        ProviderAvailability provider = ProviderAvailability.builder()
                .id(1L)
                .day_of_week(5)
                .start_time(LocalTime.now())
                .end_time(LocalTime.now())
                .provider(user)
                .build();

        when(repository.findById(provider.getId())).thenReturn(Optional.of(provider));

        ProviderAvailabilityResponseDTO responseDTO = service.findProviderById(1L);

        assertAll(
                () -> assertEquals(provider.getId(), responseDTO.id()),
                () -> assertEquals(provider.getDay_of_week(), responseDTO.day_of_week()),
                () -> assertEquals(provider.getEnd_time(), responseDTO.end_time()),
                () -> assertEquals(provider.getProvider(), responseDTO.provider()),
                () -> assertEquals(provider.getStart_time(), responseDTO.start_time()));
    }

    @Test
    void shouldReturnErrorOnGetPedidoById() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.findProviderById(1L);
        });
        assertAll(
                () -> assertEquals("Provider availability not found", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));
    }

    @Test
    void shouldDeleteProviderAvailability() {
        Users user = Users.builder()
                .id(1L)
                .email("email@email.com")
                .name("user1")
                .keycloakId("kc-test-123")
                .roles(RolesENUM.PROVIDER)
                .build();

        ProviderAvailability provider = ProviderAvailability.builder()
                .id(1L)
                .day_of_week(5)
                .start_time(LocalTime.now())
                .end_time(LocalTime.now())
                .provider(user)
                .build();

        when(repository.findById(provider.getId())).thenReturn(Optional.of(provider));
        doNothing().when(repository).deleteById(1L);

        ProviderAvailabilityResponseDTO responseDTO = service.deleteProviderById(provider.getId());

        assertAll(
                () -> assertEquals(provider.getId(), responseDTO.id()),
                () -> assertEquals(provider.getDay_of_week(), responseDTO.day_of_week()),
                () -> assertEquals(provider.getEnd_time(), responseDTO.end_time()),
                () -> assertEquals(provider.getProvider(), responseDTO.provider()),
                () -> assertEquals(provider.getStart_time(), responseDTO.start_time()));

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerProvider, times(1))
                .sendProviderDeleteEvent(any(ProviderAvailabilityDeletedEvent.class));
    }

    @Test
    void shoulReturnErrorOnDeleteProvider() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> {
            service.deleteProviderById(1L);
        });
        assertAll(
                () -> assertEquals("Provider availability not found", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));
    }

    @Test
    void shouldUpdateProviderAvailabilitySuccessfully() {

        Users user = Users.builder()
                .id(10L)
                .email("provider@test.com")
                .name("Provider")
                .keycloakId("kc-test-123")
                .roles(RolesENUM.PROVIDER)
                .build();

        ProviderAvailability existing = ProviderAvailability.builder()
                .id(1L)
                .day_of_week(2)
                .start_time(LocalTime.of(8, 0))
                .end_time(LocalTime.of(12, 0))
                .provider(user)
                .build();

        ProviderAvailabilityRequestDTO request = new ProviderAvailabilityRequestDTO(
                6,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                10L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(usersRepository.findById(10L)).thenReturn(Optional.of(user));
        when(repository.save(any(ProviderAvailability.class))).thenAnswer(inv -> inv.getArgument(0));

        // EXECUTE
        ProviderAvailabilityResponseDTO response = service.updateProvider(1L, request);

        // VERIFY
        assertAll(
                () -> assertEquals(1L, response.id()),
                () -> assertEquals(6, response.day_of_week()),
                () -> assertEquals(LocalTime.of(14, 0), response.start_time()),
                () -> assertEquals(LocalTime.of(18, 0), response.end_time()),
                () -> assertEquals(user, response.provider()));

        // repository interactions
        verify(repository, times(1)).findById(1L);
        verify(usersRepository, times(1)).findById(10L);
        verify(repository, times(1)).save(existing);

        // Evento enviado ao RabbitMQ
        verify(messageProducerProvider, times(1))
                .sendProviderUpdateEvent(any(ProviderAvailabilityUpdatedEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenProviderAvailabilityNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ProviderAvailabilityRequestDTO request = new ProviderAvailabilityRequestDTO(
                5, LocalTime.now(), LocalTime.now(), 1L);

        Assertions.assertThrows(ApiException.class, () -> {
            service.updateProvider(99L, request);
        });

        verify(repository, times(1)).findById(99L);
        verify(messageProducerProvider, never()).sendProviderUpdateEvent(any());
    }

    @Test
    void shouldGetAllProvider() {
        Users user = Users.builder()
                .id(1L)
                .email("email@email.com")
                .name("user1")
                .keycloakId("kc-test-123")
                .roles(RolesENUM.PROVIDER)
                .build();

        ProviderAvailability provider = ProviderAvailability.builder()
                .id(1L)
                .day_of_week(4)
                .start_time(LocalTime.now())
                .end_time(LocalTime.now())
                .provider(user)
                .build();

        ProviderAvailability provider1 = ProviderAvailability.builder()
                .id(2L)
                .day_of_week(6)
                .start_time(LocalTime.now())
                .end_time(LocalTime.now())
                .provider(user)
                .build();

        when(repository.findAll()).thenReturn(List.of(provider, provider1));
        List<ProviderAvailabilityResponseDTO> responseDTO = service.getAllProvider();
        assertAll(
                () -> assertEquals(provider.getId(), responseDTO.get(0).id()),
                () -> assertEquals(provider.getDay_of_week(), responseDTO.get(0).day_of_week()),
                () -> assertEquals(provider.getEnd_time(), responseDTO.get(0).end_time()),
                () -> assertEquals(provider.getProvider(), responseDTO.get(0).provider()),
                () -> assertEquals(provider.getStart_time(), responseDTO.get(0).start_time()));
    }

    @Test
    void shouldReturnErrorOnGetAllProviders() {
        when(repository.findAll()).thenReturn(List.of());
        ApiException exception = assertThrows(ApiException.class, () -> {
            service.getAllProvider();
        });
        assertAll(
                () -> assertEquals("Provider availability not found", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));
    }
}
