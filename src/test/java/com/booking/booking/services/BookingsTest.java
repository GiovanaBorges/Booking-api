package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.StatusENUM;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.BookingMapper;
import com.booking.booking.mappers.events.BookingEventMapper;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.helpers.BookingsResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

@ExtendWith(MockitoExtension.class)
public class BookingsTest {
    @Mock
    private BookingsRepository bookingsRepository;

    @Mock
    private UsersRepository userRepository;

    @Mock
    private MessageProducerBookings messageProducerBookings;

    @InjectMocks
    private BookingsServices service;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BookingMapper bookingsMapper;

    @Mock
    private BookingEventMapper bookingsEventMapper;

    @Mock
    private BookingsResolver bookingsResolver;


    @BeforeEach
    void setup() {

    Users user = Users.builder()
            .id(1L)
            .email("client@email.com")
            .name("client")
            .keycloakId("kc-client-123")
            .roles(RolesENUM.CLIENT)
            .build();

    Users provider = Users.builder()
            .id(2L)
            .email("provider@email.com")
            .name("provider")
            .keycloakId("kc-provider-123")
            .roles(RolesENUM.PROVIDER)
            .build();

    Bookings booking = Bookings.builder()
            .id(1L)
            .customer(user)
            .provider(provider)
            .startsTs(LocalDateTime.of(2026,1,10,10,0))
            .endTs(LocalDateTime.of(2026,1,10,11,0))
            .build();

        BookingsRequestDTO request = new BookingsRequestDTO(
                provider.getId(),
                user.getId(),
                LocalDateTime.of(2026,1,10,10,0),
                LocalDateTime.of(2026,1,11,11,0),
                StatusENUM.CONFIRMED
        );

    BookingsResponseDTO response = new BookingsResponseDTO(
            1L,
            provider.getId(),
            user.getId(),
            LocalDateTime.of(2026,1,10,10,0),
            LocalDateTime.of(2026,1,10,11,0),
            StatusENUM.CONFIRMED,
            LocalDateTime.of(2026,1,10,11,0),
            LocalDateTime.of(2026,1,10,11,0)
    );

    // ========================
    // bookingsRepository mocks
    // ========================

    lenient().when(bookingsRepository.findConflict(any(), any(), any()))
            .thenReturn(Optional.empty());

    lenient().when(bookingsRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // ========================
    // Mapper mocks
    // ========================

    lenient().when(bookingsMapper.toEntity(any(), any()))
            .thenReturn(booking);

    lenient().when(bookingsMapper.toResponse(any()))
            .thenReturn(response);

    // ========================
    // Event mapper
    // ========================

    lenient().when(bookingsEventMapper.toCreatedEvent(any()))
            .thenReturn(BookingCreatedEvent.builder().build());

}

    @Test
    void shouldCreateBooking(){
        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .keycloakId("kc-test-123")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .keycloakId("kc-test-124")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();

        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .build();

            BookingsRequestDTO request = new BookingsRequestDTO(
                booking.getProvider().getId(),
                booking.getCustomer().getId(),
                booking.getStartsTs(),
                booking.getEndTs(),
                booking.getStatus());


        // ==== MOCKS ====
            when(bookingsRepository.findConflict(any(), any(), any()))
        .thenReturn(Optional.empty());

    when(bookingsRepository.save(any(Bookings.class)))
        .thenAnswer(inv -> {
            Bookings b = inv.getArgument(0);
            b.setId(1L); // Simula ID ao salvar
            return b;
        });

    when(bookingsMapper.toEntity(any(), any()))
        .thenAnswer(inv -> {
            BookingsRequestDTO r = inv.getArgument(0);
            return Bookings.builder()
                .provider(provider)
                .customer(customer)
                .startsTs(r.startsTs())
                .endTs(r.endTs())
                .status(r.status())
                .build();
        });

    when(bookingsMapper.toResponse(any(Bookings.class)))
        .thenAnswer(inv -> {
            Bookings b = inv.getArgument(0);
            return new BookingsResponseDTO(
                b.getId(),
                b.getProvider().getId(),
                b.getCustomer().getId(),
                b.getStartsTs(),
                b.getEndTs(),
                b.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        });

    when(bookingsEventMapper.toCreatedEvent(any(Bookings.class)))
        .thenAnswer(inv -> {
            Bookings b = inv.getArgument(0);
            return BookingCreatedEvent.builder()
                .id(b.getId())
                .providerId(b.getProvider().getId())
                .customerId(b.getCustomer().getId())
                .startsTs(b.getStartsTs())
                .endTs(b.getEndTs())
                .build();
        });        
        
        BookingsResponseDTO responseDTO = service.saveBooking(request);
             
        assertEquals(booking.getId(), responseDTO.id());
 
        verify(bookingsRepository).save(any(Bookings.class));
        verify(messageProducerBookings).sendBookingCreateEvent(any(BookingCreatedEvent.class));
    }

    @Test
    void shouldFindBookingById(){
        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .keycloakId("kc-test-123")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .keycloakId("kc-test-1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .provider(provider)
            .customer(customer)
            .startsTs(LocalDateTime.now())
            .build();

            when(bookingsResolver.resolveBookingById(booking.getId()))
            .thenReturn(Optional.of(booking));

        when(bookingsMapper.toResponse(any()))
        .thenAnswer(inv -> {
            Bookings b = inv.getArgument(0);
            return new BookingsResponseDTO(
                b.getId(),
                b.getProvider().getId(),
                b.getCustomer().getId(),
                b.getStartsTs(),
                b.getEndTs(),
                b.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        });

        BookingsResponseDTO responseDTO = service.getBookingById(booking.getId());

        assertAll(
            () -> assertEquals(booking.getId(), responseDTO.id()),
            () -> assertEquals(booking.getStartsTs(), responseDTO.startsTs()),
            () -> assertEquals(booking.getEndTs(), responseDTO.endTs()),
            () -> assertEquals(booking.getStatus(), responseDTO.status()),
            () -> assertEquals(booking.getCustomer().getId(), responseDTO.customer()),
            () -> assertEquals(booking.getProvider().getId(), responseDTO.provider())
        );
    }

    @Test
    void shouldReturnErrorOnGetBookingById(){
        when(bookingsResolver.resolveBookingById(1L)).thenReturn(Optional.empty());
        
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.getBookingById(1L);
        });
      
        assertEquals("BOOKINGS NOT FOUND", exception.getMessage());
        verify(bookingsResolver).resolveBookingById(1L);
    }

    @Test
    void shouldDeleteBookings(){
         Users provider = Users.builder()
        .id(1L)
        .email("email@provider.com")
        .name("user1")
        .keycloakId("kc-test-123")
        .roles(RolesENUM.PROVIDER)
        .createdAt(LocalDateTime.now())
        .build();

    Users customer = Users.builder()
        .id(2L)
        .email("email@customer.com")
        .name("user2")
        .keycloakId("kc-test-1234")
        .roles(RolesENUM.CLIENT)
        .createdAt(LocalDateTime.now())
        .build();

    Bookings booking = Bookings.builder()
        .id(1L)
        .customer(customer)
        .provider(provider)
        .startsTs(LocalDateTime.now())
        .build();

    Optional<Bookings> bookingOptional = Optional.of(booking);

    when(bookingsResolver.resolveBookingById(1L))
        .thenReturn(bookingOptional);

    when(bookingsEventMapper.toDeletedEvent(booking))
        .thenReturn(BookingDeletedEvent.builder()
            .id(booking.getId())
            .customerId(customer.getId())
            .providerId(provider.getId())
            .startsTs(booking.getStartsTs())
            .endTs(booking.getEndTs())
            .build());

    when(bookingsMapper.toResponse(booking))
        .thenReturn(new BookingsResponseDTO(
            booking.getId(),
            provider.getId(),
            customer.getId(),
            booking.getStartsTs(),
            booking.getEndTs(),
            booking.getStatus(),
            LocalDateTime.now(),
            LocalDateTime.now()
        ));

    // EXECUTE
    BookingsResponseDTO responseDTO = service.deleteBooking(1L);

    // ASSERT
    assertEquals(booking.getId(), responseDTO.id());

    // VERIFY
    verify(bookingsRepository).deleteById(1L);
    verify(messageProducerBookings)
        .sendBookingDeleteEvent(any(BookingDeletedEvent.class));
    }

    @Test
    void shoulReturnErrorOnDeleteBooking(){
        when(bookingsResolver.resolveBookingById(1L))
            .thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->{
            service.deleteBooking(1L);
        });

            assertEquals("BOOKINGS NOT FOUND", exception.getMessage());

        verify(messageProducerBookings, never())
            .sendBookingDeleteEvent(any());
        verify(bookingsRepository, never()).deleteById(any());
    }

    @Test
    void shouldUpdateBookingsuccessfully() {

       Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .keycloakId("kc-test-123")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .keycloakId("kc-test-1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .build();

        BookingsRequestDTO requestDTO = new BookingsRequestDTO(
               booking.getProvider().getId(),
               booking.getCustomer().getId(),
               booking.getStartsTs(),
               booking.getEndTs(),
               booking.getStatus()
        );

        when(bookingsResolver.resolveBookingById(1L))
            .thenReturn(Optional.of(booking));

    when(bookingsRepository.save(any(Bookings.class)))
            .thenReturn(booking);

    when(bookingsEventMapper.toUpdatedEvent(any(Bookings.class)))
            .thenReturn(BookingUpdatedEvent.builder().build());

    when(bookingsMapper.toResponse(any(Bookings.class)))
            .thenReturn(new BookingsResponseDTO(
                booking.getId(),
                booking.getProvider().getId(),
                booking.getCustomer().getId(),
                booking.getStartsTs(),
                booking.getEndTs(),
                booking.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ));
        // EXECUTE
        BookingsResponseDTO responseDTO = service.updateBooking(1L, requestDTO);

        // VERIFY
        assertEquals(booking.getId(), responseDTO.id());

        // bookingsRepository interactions
        verify(bookingsResolver).resolveBookingById(1L);
        verify(bookingsRepository).save(any(Bookings.class));

        // Evento enviado ao RabbitMQ
        verify(messageProducerBookings)
            .sendBookingUpdateEvent(any(BookingUpdatedEvent.class));

        
    }

    @Test
    void shouldGetAllBookings(){

         Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .keycloakId("kc-test-123")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .keycloakId("kc-test-1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .endTs(LocalDateTime.now().plusHours(1))
            .build();

        Bookings booking1 = Bookings.builder()
            .id(2L)
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        when(bookingsRepository.findAll()).thenReturn(List.of(booking,booking1));
        List<BookingsResponseDTO> responseDTO = service.getAllBookings();

        assertEquals(2, responseDTO.size());

        verify(bookingsRepository).findAll();
        verify(bookingsMapper,times(responseDTO.size())).toResponse(any(Bookings.class));
    }

    @Test
    void shouldReturnErrorOnGetAllBookings(){
        when(bookingsRepository.findAll())
            .thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, 
            () -> service.getAllBookings()); 

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(bookingsRepository).findAll();
    }
}
