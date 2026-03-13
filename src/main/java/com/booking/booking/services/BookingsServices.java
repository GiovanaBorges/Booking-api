package com.booking.booking.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.BookingMapper;
import com.booking.booking.mappers.events.BookingEventMapper;
import com.booking.booking.models.Bookings;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.helpers.BookingsResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class BookingsServices {
    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private MessageProducerBookings messageProducerBookings;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private BookingEventMapper bookingEventMapper;

    @Autowired
    private BookingsResolver bookingsResolver;

    private static final Logger LOG =
    LoggerFactory.getLogger(BookingsServices.class);

    @Bulkhead(name = "bookingsBulkhead")
    @CircuitBreaker(name = "bookingsCircuitBreaker", fallbackMethod = "handleBookingsEventFailure")
    @RateLimiter(name = "bookingsRateLimiter")
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO saveBooking(BookingsRequestDTO requestDTO){
        
            Optional<Bookings> conflict = bookingsRepository.findConflict(
                requestDTO.providerId(), 
                requestDTO.startsTs(),
                requestDTO.endTs()
            );

            if(conflict.isPresent()){
                throw new ApiException("THIS TIME SLOT IS ALREADY BOOKED",
                 HttpStatus.CONFLICT);
            }
     
        Bookings booking = bookingMapper.toEntity(requestDTO, usersRepository);
        
        Bookings bookingSaved = bookingsRepository.save(booking);

        BookingCreatedEvent bookingCreatedEvent = bookingEventMapper.toCreatedEvent(bookingSaved);

        messageProducerBookings.sendBookingCreateEvent(bookingCreatedEvent);

        return bookingMapper.toResponse(bookingSaved);
    }

    @Bulkhead(name = "bookingsBulkhead")
    @CircuitBreaker(name = "bookingsCircuitBreaker", fallbackMethod = "handleBookingsEventFailure")
    @Retry(name = "bookingsRetry", fallbackMethod = "handleBookingsEventFailure")
    @RateLimiter(name = "bookingsRateLimiter")
    @Cacheable(value = "bookings", key = "#id")
    public BookingsResponseDTO getBookingById(Long id){
        Bookings bookingsFound = bookingsResolver.resolveBookingById(id)
            .orElseThrow(() -> new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));

        return bookingMapper.toResponse(bookingsFound);
    }

    @Bulkhead(name = "bookingsBulkhead")
    @CircuitBreaker(name = "bookingsCircuitBreaker", fallbackMethod = "handleBookingsEventFailure")
    @RateLimiter(name = "bookingsRateLimiter")
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO deleteBooking(Long id){
        Bookings bookingsFound = bookingsResolver.resolveBookingById(id)
                        .orElseThrow(() -> new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));
        bookingsRepository.deleteById(id);

        BookingDeletedEvent bookingDeletedEvent = bookingEventMapper.toDeletedEvent(bookingsFound);

        messageProducerBookings.sendBookingDeleteEvent(bookingDeletedEvent);
            
        return bookingMapper.toResponse(bookingsFound);
    }

    @Bulkhead(name = "bookingsBulkhead")
    @CircuitBreaker(name = "bookingsCircuitBreaker", fallbackMethod = "handleBookingsEventFailure")
    @RateLimiter(name = "bookingsRateLimiter")
    public BookingsResponseDTO updateBooking(Long id,BookingsRequestDTO bookingsRequestDTO){
        
        Optional<Bookings> booking = bookingsResolver.resolveBookingById(id);

        bookingMapper.updateEntity(bookingsRequestDTO,booking.get(),usersRepository);
        
        Bookings saved = bookingsRepository.save(booking.get());

        BookingUpdatedEvent event = bookingEventMapper.toUpdatedEvent(saved);

        messageProducerBookings.sendBookingUpdateEvent(event);

        return bookingMapper.toResponse(saved);
    }

    @Bulkhead(name = "bookingsBulkhead")
    @CircuitBreaker(name = "bookingsCircuitBreaker", fallbackMethod = "handleBookingsEventFailure")
    @Retry(name = "bookingsRetry", fallbackMethod = "handleBookingsEventFailure")
    @RateLimiter(name = "bookingsRateLimiter")
    @Cacheable(value = "bookings", key = "'all'")
    public List<BookingsResponseDTO> getAllBookings(){
        List<Bookings> bookings = bookingsRepository.findAll();

        if(bookings.isEmpty()){
            throw new ApiException("NO BOOKINGS FOUND",HttpStatus.NOT_FOUND);
        }
        return bookings.stream()
            .map(bookingMapper::toResponse)
            .toList();

    }   

    // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t){
        LOG.error("Booking service fallback triggered", t);
        return new ApiException(
            "BOOKING SERVICE TEMPORARILY UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    // saveBooking
    public BookingsResponseDTO handleBookingsEventFailure(BookingsRequestDTO dto, Throwable t){
        throw serviceUnavailable(t);
    }

    // getBookingById e deleteBooking
    public BookingsResponseDTO handleBookingsEventFailure(Long id, Throwable t){
        throw serviceUnavailable(t);
    }

    // updateBooking
    public BookingsResponseDTO handleBookingsEventFailure(Long id, BookingsRequestDTO dto, Throwable t){
        throw serviceUnavailable(t);
    }

    // getAllBookings
    public List<BookingsResponseDTO> handleBookingsEventFailure(Throwable t){
        throw serviceUnavailable(t);
    }
}
