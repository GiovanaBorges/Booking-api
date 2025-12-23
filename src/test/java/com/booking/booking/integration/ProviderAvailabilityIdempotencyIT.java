package com.booking.booking.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.booking.booking.DTO.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.ProviderAvailabilityservices;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class ProviderAvailabilityIdempotencyIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.data.redis.host",
                redis::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ProviderAvailabilityservices service;

    @Autowired
    private ProviderAvailabilityRepository providerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private MessageProducerProvider messageProducerProvider;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldBlockSecondRequestWithSameIdempotencyKey() {

        // ===== GIVEN =====
        Users user = usersRepository.save(
            Users.builder()
                .email("email@email.com")
                .name("user1")
                .keycloakId("keycloak-key-123")
                .roles(RolesENUM.PROVIDER)
                .build()
        );

        ProviderAvailabilityRequestDTO request =
            new ProviderAvailabilityRequestDTO(
                1,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                user.getId()
            );

        String idempotencyKey = "idem-integration-123";

        // ===== WHEN =====
        ProviderAvailabilityResponseDTO firstResponse =
            service.saveProviderAvailability(request, idempotencyKey);

        // ===== THEN (1ª chamada ok) =====
        assertNotNull(firstResponse.id());
        assertEquals(1, providerRepository.count());

        verify(messageProducerProvider, times(1))
            .sendProviderCreateEvent(any());

       ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> {
            service.saveProviderAvailability(request, idempotencyKey);
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Request already processed", ex.getReason());

        assertEquals(1, providerRepository.count());

        verify(messageProducerProvider, times(1))
            .sendProviderCreateEvent(any());

       
    }
}
