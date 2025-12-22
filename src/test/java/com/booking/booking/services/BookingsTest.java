package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

import com.booking.booking.DTO.BookingsRequestDTO;
import com.booking.booking.DTO.BookingsResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

@ExtendWith(MockitoExtension.class)
public class BookingsTest {
    @Mock
    private BookingsRepository repository;

    @Mock
    private UsersRepository Userrepository;

    @Mock
    private MessageProducerBookings messageProducerBookings;

    @InjectMocks
    private BookingsServices service;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private LockService lockService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateBooking(){
        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .build();

        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();
            
        when(repository.save(any(Bookings.class))).thenAnswer(i ->{
            Bookings b = i.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(Userrepository.findById(1L))
            .thenReturn(Optional.of(provider));

        when(Userrepository.findById(2L))
            .thenReturn(Optional.of(customer));

        when(redisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(lockService.acquireLock(any(), any())).thenReturn(true);
        doNothing().when(lockService).releaseLock(any());

        BookingsResponseDTO responseDTO = service.saveBooking(
            new BookingsRequestDTO(
                booking.getId(),
                booking.getProvider().getId(),
                booking.getStartsTs(),
                booking.getEndTs(),
                booking.getStatus()
            ),"idem-key-123"
        );

        assertAll(
            () -> assertEquals(booking.getId(), responseDTO.id()),
            () -> assertEquals(booking.getStartsTs(), responseDTO.startsTs()),
            () -> assertEquals(booking.getEndTs(), responseDTO.endTs()),
            () -> assertEquals(booking.getStatus(), responseDTO.status()),
            () -> assertEquals(booking.getCustomer(), responseDTO.customer()),
            () -> assertEquals(booking.getProvider(), responseDTO.provider())
        );

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerBookings, times(1))
            .sendBookingCreateEvent(any(BookingCreatedEvent.class));
    }

    @Test
    void shouldFindBookingById(){
        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

    
        when(repository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BookingsResponseDTO responseDTO = service.getBookingById(1L);

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
        when(repository.findById(1L)).thenReturn(Optional.empty());
        
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.getBookingById(1L);
        });
        assertAll(
            () -> assertEquals("BOOKINGS NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }

    @Test
    void shouldDeleteBookings(){
         Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        
        when(repository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(repository).deleteById(1L);

        BookingsResponseDTO responseDTO = service.deleteBooking(booking.getId());

        assertAll(
            () -> assertEquals(booking.getId(), responseDTO.id()),
            () -> assertEquals(booking.getStartsTs(), responseDTO.startsTs()),
            () -> assertEquals(booking.getEndTs(), responseDTO.endTs()),
            () -> assertEquals(booking.getStatus(), responseDTO.status()),
            () -> assertEquals(booking.getCustomer().getId(), responseDTO.customer()),
            () -> assertEquals(booking.getProvider().getId(), responseDTO.provider())
        );

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerBookings, times(1))
            .sendBookingDeleteEvent(any(BookingDeletedEvent.class));
    }

    @Test
    void shoulReturnErrorOnDeleteBooking(){
        when(repository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.deleteBooking(1L);
        });

        assertAll(
            () -> assertEquals("BOOKINGS NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }

    @Test
    void shouldUpdateBookingsuccessfully() {

       Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        BookingsRequestDTO requestDTO = new BookingsRequestDTO(
               booking.getProvider().getId(),
               booking.getCustomer().getId(),
               booking.getStartsTs(),
               booking.getEndTs(),
               booking.getStatus()
        );

        when(Userrepository.findById(1L))
            .thenReturn(Optional.of(provider));

        when(Userrepository.findById(2L))
            .thenReturn(Optional.of(customer));

        when(repository.findById(1L)).thenReturn(Optional.of(booking));
        when(repository.save(any(Bookings.class))).thenAnswer(inv -> inv.getArgument(0));

        // EXECUTE
        BookingsResponseDTO responseDTO = service.updateBooking(1L, requestDTO);

        // VERIFY
        assertAll(
            () -> assertEquals(booking.getId(), responseDTO.id()),
            () -> assertEquals(booking.getStartsTs(), responseDTO.startsTs()),
            () -> assertEquals(booking.getEndTs(), responseDTO.endTs()),
            () -> assertEquals(booking.getStatus(), responseDTO.status()),
            () -> assertEquals(booking.getCustomer().getId(), responseDTO.customer()),
            () -> assertEquals(booking.getProvider().getId(), responseDTO.provider())
        );

        // repository interactions
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(booking);

        // Evento enviado ao RabbitMQ
        verify(messageProducerBookings, times(1))
            .sendBookingUpdateEvent(any(BookingUpdatedEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenBookingsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        BookingsRequestDTO requestDTO = new BookingsRequestDTO(
               booking.getProvider().getId(),
               booking.getCustomer().getId(),
               booking.getStartsTs(),
               booking.getEndTs(),
               booking.getStatus()
        );

        Assertions.assertThrows(ApiException.class, () -> {
            service.updateBooking(99L, requestDTO);
        });

        verify(repository, times(1)).findById(99L);
        verify(messageProducerBookings, never()).sendBookingUpdateEvent(any());
    }

    @Test
    void shouldGetAllBookings(){

        Users provider = Users.builder()
            .id(1L)
            .email("email@provider.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        Users customer = Users.builder()
            .id(2L)
            .email("email@customer.com")
            .name("user2")
            .password("1234")
            .roles(RolesENUM.CLIENT)
            .createdAt(LocalDateTime.now())
            .build();


        Bookings booking = Bookings.builder()
            .id(1L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        Bookings booking1 = Bookings.builder()
            .id(2L)
            .startsTs(LocalDateTime.now())
            .customer(customer)
            .provider(provider)
            .startsTs(LocalDateTime.now())
            .build();

        when(repository.findAll()).thenReturn(List.of(booking,booking1));
        List<BookingsResponseDTO> responseDTO = service.getAllBookings();

        assertAll(
            () -> assertEquals(booking.getId(), responseDTO.get(0).id()),
            () -> assertEquals(booking.getStartsTs(), responseDTO.get(0).startsTs()),
            () -> assertEquals(booking.getEndTs(), responseDTO.get(0).endTs()),
            () -> assertEquals(booking.getStatus(), responseDTO.get(0).status()),
            () -> assertEquals(booking.getCustomer().getId(), responseDTO.get(0).customer()),
            () -> assertEquals(booking.getProvider().getId(), responseDTO.get(0).provider())
        );

    }

    @Test
    void shouldReturnErrorOnGetAllBookings(){
        when(repository.findAll()).thenReturn(List.of());
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.getAllBookings();
        });
         assertAll(
            () -> assertEquals("BOOKINGS NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }
}
