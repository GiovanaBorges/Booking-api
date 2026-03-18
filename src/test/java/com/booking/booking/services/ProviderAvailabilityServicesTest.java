package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.ProviderAvailabilityMapper;
import com.booking.booking.mappers.events.ProviderAvailabilityEventMapper;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.services.helpers.ProviderAvailabilityResolver;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

@ExtendWith(MockitoExtension.class)
public class ProviderAvailabilityServicesTest {

    @Mock
    private ProviderAvailabilityRepository repository;

    @Mock
    private MessageProducerProvider messageProducerProvider;

    @InjectMocks
    private ProviderAvailabilityservices service;

    @Mock 
    private UserResolver userResolver;

    @Mock
    private ProviderAvailabilityMapper providerAvailabilityMapper;

    @Mock
    private ProviderAvailabilityEventMapper providerEventMapper;

    @Mock
    private ProviderAvailabilityCreatedEvent providerAvailabilityCreatedEvent;

    @Mock
    private ProviderAvailabilityDeletedEvent providerAvailabilityDeletedEvent;

    @Mock
    private ProviderAvailabilityUpdatedEvent providerAvailabilityUpdatedEvent;

    @Mock
    private ProviderAvailabilityResolver providerAvailabilityResolver;

    @BeforeEach
    void setup() {
        
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
            .start_time(LocalTime.of(9,0))
            .end_time(LocalTime.of(18,0))
            .provider(user)
            .build();

    lenient().when(userResolver.resolveUserById(any()))
            .thenReturn(user);

    lenient().when(providerAvailabilityResolver.resolveProviderById(any()))
            .thenReturn(provider);

    lenient().when(repository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));

    lenient().when(providerAvailabilityMapper.toEntity(any()))
            .thenReturn(provider);

    lenient().when(providerAvailabilityMapper.toResponse(any()))
            .thenReturn(
                    new ProviderAvailabilityResponseDTO(
                            1L,
                            5,
                            LocalTime.of(9,0),
                            LocalTime.of(18,0),
                            user
                    )
            );

    lenient().when(providerEventMapper.toCreateEvent(any()))
            .thenReturn(
                ProviderAvailabilityCreatedEvent.builder().build());

    lenient().when(providerEventMapper.toDeletedEvent(any()))
            .thenReturn(ProviderAvailabilityDeletedEvent.builder().build());

    lenient().when(providerEventMapper.toUpdatedEvent(any()))
            .thenReturn(ProviderAvailabilityUpdatedEvent.builder().build());
    }

    @Test
    void shouldCreateProvider() {

        ProviderAvailabilityRequestDTO request =
            new ProviderAvailabilityRequestDTO(
                    5,
                    LocalTime.of(9,0),
                    LocalTime.of(18,0),
                    1L);

        ProviderAvailabilityResponseDTO response =
            service.saveProviderAvailability(request);

         assertAll(
            () -> assertEquals(1L, response.id()),
            () -> assertEquals(5, response.day_of_week())
    );

    verify(repository).save(any());
    verify(messageProducerProvider)
            .sendProviderCreateEvent(any(ProviderAvailabilityCreatedEvent.class));
    }

    
    @Test
    void shouldFindProviderById() {
       ProviderAvailabilityResponseDTO response = service.findProviderById(1L);
    
        assertAll(
                () -> assertEquals(1L, response.id()),
                () -> assertEquals(5, response.day_of_week())
        );

        verify(providerAvailabilityResolver).resolveProviderById(1L);
        verify(providerAvailabilityMapper).toResponse(any());
    }

    @Test
    void shouldReturnErrorOnGetProviderById() {
        when(providerAvailabilityResolver.resolveProviderById(99L))
                .thenThrow(
                        new ApiException("PROVIDER AVAILABILITY NOT FOUND"
                        , HttpStatus.NOT_FOUND));

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.findProviderById(99L);
        });
        assertAll(
                () -> assertEquals("PROVIDER AVAILABILITY NOT FOUND", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));

        verify(providerAvailabilityResolver).resolveProviderById(99L);
        verify(providerAvailabilityMapper, never()).toResponse(any());
    }

    @Test
    void shouldDeleteProviderAvailability() {
        service.deleteProviderById(1L);

        verify(providerAvailabilityResolver).resolveProviderById(1L);
        verify(repository).deleteById(1L);
        verify(providerEventMapper)
            .toDeletedEvent(any(ProviderAvailability.class));
        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerProvider)
                .sendProviderDeleteEvent(any(ProviderAvailabilityDeletedEvent.class));
    }

    @Test
    void shoulReturnErrorOnDeleteProvider() {
        when(providerAvailabilityResolver.resolveProviderById(1L)).thenThrow
        (new ApiException("PROVIDER AVAILABILITY NOT FOUND", HttpStatus.NOT_FOUND));
        ApiException exception = assertThrows(ApiException.class, () -> {
            service.deleteProviderById(1L);
        });
        assertAll(
                () -> assertEquals("PROVIDER AVAILABILITY NOT FOUND", exception.getMessage()),
                () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus()));
    }

    @Test
    void shouldUpdateProviderAvailabilitySuccessfully() {

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
            .start_time(LocalTime.of(9,0))
            .end_time(LocalTime.of(18,0))
            .provider(user)
            .build();

        ProviderAvailabilityRequestDTO request = new ProviderAvailabilityRequestDTO(
                6,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                10L);

        when(providerAvailabilityResolver.resolveProviderById(1L)).thenReturn(provider);
        
        ProviderAvailabilityResponseDTO response =
            service.updateProvider(1L,request);

        // VERIFY
       assertEquals(provider.getId(), response.id());

        // repository interactions
        verify(providerAvailabilityResolver, times(1)).resolveProviderById(1L);
        verify(repository, times(1)).save(provider);
        verify(providerAvailabilityMapper, times(1)).updateEntity(request, provider);

        verify(providerEventMapper, times(1)).toUpdatedEvent(provider);
        // Evento enviado ao RabbitMQ
        verify(messageProducerProvider, times(1))
                .sendProviderUpdateEvent(any(ProviderAvailabilityUpdatedEvent.class));
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

        assertEquals(2, responseDTO.size());
        verify(repository,times(1)).findAll();
    }

    @Test
    void shouldReturnErrorOnGetAllProviders() {
        when(repository.findAll()).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, 
                () -> service.getAllProvider());

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(repository,times(1)).findAll();
    }
}
