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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.StatusENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.AuthenticatedUserService;
import com.booking.booking.services.BookingsServices;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BookingsIntegrationTest {
    @Autowired
    private BookingsServices service;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @MockBean
    private MessageProducerBookings messageProducerBookings;

    @MockBean
    private AuthenticatedUserService authUserService;

    private Users provider;
    private Users client;

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

        client = usersRepository.save(
                Users.builder()
                        .id(1L)
                        .email("client@email.com")
                        .name("client")
                        .keycloakId("kc-client-123")
                        .roles(RolesENUM.CLIENT)
                        .build());

        when(authUserService.getAuthenticatedUser())
                .thenReturn(client);
    }

    @Test
    void shouldCreateBookingIntegration() {

        BookingsRequestDTO request = new BookingsRequestDTO(
                provider.getId(),
                client.getId(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                StatusENUM.CONFIRMED,
                "Test Booking",
                "Description Booking");

        // EXECUTE
        BookingsResponseDTO response = service.saveBooking(request);

        // ASSERT
        assertNotNull(response.id());
        assertEquals(provider.getId(), response.provider());
        assertEquals(client.getId(), response.customer());

        // valida banco real
        List<Bookings> bookings = bookingsRepository.findAll();
        assertEquals(1, bookings.size());

        Bookings saved = bookings.get(0);

        assertEquals("Test Booking", saved.getTitle());
        assertEquals(client.getId(), saved.getCustomer().getId());

        // verifica evento , único mock
        verify(messageProducerBookings).sendEvent(any());
    }

    @Test
    void shouldFindBookingByIdIntegration() {

        // save booking
        Bookings booking = bookingsRepository.save(
                Bookings.builder()
                        .provider(provider)
                        .customer(client)
                        .startTs(LocalDateTime.now())
                        .endTs(LocalDateTime.now().plusHours(1))
                        .status(StatusENUM.CONFIRMED)
                        .title("Test Booking")
                        .description("Desc")
                        .build());

        // EXECUTE
        BookingsResponseDTO response = service.getBookingById(booking.getId());

        // ASSERT
        assertAll(
                () -> assertEquals(booking.getId(), response.id()),
                () -> assertEquals(booking.getStartTs(), response.startTs()),
                () -> assertEquals(booking.getEndTs(), response.endTs()),
                () -> assertEquals(booking.getStatus(), response.status()),
                () -> assertEquals(client.getId(), response.customer()),
                () -> assertEquals(provider.getId(), response.provider()));
    }

    @Test
    void shouldReturnErrorOnGetBookingByIdIntegration() {

        // não salva nada no banco
        ApiException exception = assertThrows(ApiException.class, () -> {
            service.getBookingById(999L);
        });

        assertEquals("BOOKING NOT FOUND", exception.getMessage());
    }

    @Test
    void shouldDeleteBookingsIntegration() {

        Bookings booking = bookingsRepository.save(
                Bookings.builder()
                        .provider(provider)
                        .customer(client)
                        .startTs(LocalDateTime.now())
                        .endTs(LocalDateTime.now().plusHours(1))
                        .status(StatusENUM.CONFIRMED)
                        .title("Test")
                        .description("Desc")
                        .build());

        // EXECUTE
        BookingsResponseDTO response = service.deleteBooking(booking.getId());

        // ASSERT
        assertEquals(booking.getId(), response.id());

        // valida que foi removido do banco
        Optional<Bookings> deleted = bookingsRepository.findById(booking.getId());
        assertTrue(deleted.isEmpty());

        // evento disparado
        verify(messageProducerBookings).sendEvent(any());
    }

    @Test
    void shouldReturnErrorOnDeleteBookingIntegration() {

        ApiException exception = assertThrows(ApiException.class, () -> {
            service.deleteBooking(999L);
        });

        assertEquals("BOOKING NOT FOUND", exception.getMessage());

        verify(messageProducerBookings, never()).sendEvent(any());
    }

    @Test
    void shouldUpdateBookingSuccessfullyIntegration() {

        Bookings booking = bookingsRepository.save(
                Bookings.builder()
                        .provider(provider)
                        .customer(client)
                        .startTs(LocalDateTime.now())
                        .endTs(LocalDateTime.now().plusHours(1))
                        .status(StatusENUM.CONFIRMED)
                        .title("OLD")
                        .description("OLD DESC")
                        .build());

        // novo conteúdo
        BookingsRequestDTO request = new BookingsRequestDTO(
                provider.getId(),
                client.getId(),
                booking.getStartTs().plusDays(1),
                booking.getEndTs().plusDays(1),
                StatusENUM.CONFIRMED,
                "UPDATED",
                "UPDATED DESC");

        // EXECUTE
        BookingsResponseDTO response = service.updateBooking(booking.getId(), request);

        // ASSERT
        assertEquals(booking.getId(), response.id());
        assertEquals("UPDATED", response.title());

        // valida no banco
        Bookings updated = bookingsRepository.findById(booking.getId()).orElseThrow();

        assertEquals("UPDATED", updated.getTitle());
        assertEquals("UPDATED DESC", updated.getDescription());

        verify(messageProducerBookings).sendEvent(any());
    }

    @Test
    void shouldGetAllBookingsIntegration() {

        bookingsRepository.save(
                Bookings.builder()
                        .provider(provider)
                        .customer(client)
                        .startTs(LocalDateTime.now())
                        .endTs(LocalDateTime.now().plusHours(1))
                        .status(StatusENUM.CONFIRMED)
                        .title("Booking 1")
                        .build());

        bookingsRepository.save(
                Bookings.builder()
                        .provider(provider)
                        .customer(client)
                        .startTs(LocalDateTime.now())
                        .endTs(LocalDateTime.now().plusHours(2))
                        .status(StatusENUM.CONFIRMED)
                        .title("Booking 2")
                        .build());

        // EXECUTE
        List<BookingsResponseDTO> response = service.getAllBookings();

        // ASSERT
        assertEquals(2, response.size());
    }
}
