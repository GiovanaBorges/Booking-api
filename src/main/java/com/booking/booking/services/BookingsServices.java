package com.booking.booking.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.EventDTO;
import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.EventTypeEnum;
import com.booking.booking.ENUMS.StatusENUM;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.BookingMapper;
import com.booking.booking.mappers.events.BookingEventMapper;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.helpers.BookingsResolver;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class BookingsServices {
    private final BookingsRepository bookingsRepository;
    private final MessageProducerBookings messageProducerBookings;
    private final UsersRepository usersRepository;
    private final UserResolver userResolver;
    private final BookingMapper bookingMapper;
    private final BookingEventMapper bookingEventMapper;
    private final BookingsResolver bookingsResolver;
    private final AuthenticatedUserService authUserService;

    public BookingsServices(
            BookingsRepository bookingsRepository,
            MessageProducerBookings messageProducerBookings,
            UsersRepository usersRepository,
            BookingMapper bookingMapper,
            BookingEventMapper bookingEventMapper,
            BookingsResolver bookingsResolver,
            AuthenticatedUserService authUserService,
            UserResolver userResolver) {
        this.bookingsRepository = bookingsRepository;
        this.messageProducerBookings = messageProducerBookings;
        this.usersRepository = usersRepository;
        this.bookingMapper = bookingMapper;
        this.bookingEventMapper = bookingEventMapper;
        this.bookingsResolver = bookingsResolver;
        this.authUserService = authUserService;
        this.userResolver = userResolver;
    }

    private static final Logger LOG = LoggerFactory.getLogger(BookingsServices.class);

 
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO saveBooking(BookingsRequestDTO requestDTO) {

        Optional<Bookings> conflict = bookingsRepository.findConflict(
                requestDTO.providerId(),
                requestDTO.startTs(),
                requestDTO.endTs());

        if (conflict.isPresent()) {
            throw new ApiException("THIS TIME SLOT IS ALREADY BOOKED",
                    HttpStatus.CONFLICT);
        }

        Users user = authUserService.getAuthenticatedUser();
        Users provider = userResolver.resolveUserById(requestDTO.providerId());

        System.err.println("USER ID: " + user.getId());

        Bookings booking = bookingMapper.toEntity(requestDTO, usersRepository);
        booking.setCustomer(user);
        booking.setProvider(provider);

        Bookings bookingSaved = bookingsRepository.save(booking);

        EventDTO<BookingCreatedEvent> event = new EventDTO<>(
                EventTypeEnum.BOOKING_CREATED,
                bookingEventMapper.toCreatedEvent(bookingSaved),
                LocalDateTime.now(),
                List.of(booking.getProvider().getId(),booking.getCustomer().getId()));

        messageProducerBookings.sendEvent(event);

        return bookingMapper.toResponse(bookingSaved);
    }

    public List<BookingsResponseDTO> getBookingsByStatus(String status) {
        Users provider = authUserService.getAuthenticatedUser();

        return bookingsRepository
                .findByProviderAndStatus(provider, StatusENUM.valueOf(status.toUpperCase()))
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }


    @Cacheable(value = "bookings", key = "#id")
    public BookingsResponseDTO getBookingById(Long id) {
        Bookings bookingsFound = bookingsResolver.resolveBookingById(id)
                .orElseThrow(() -> new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));

        return bookingMapper.toResponse(bookingsFound);
    }

    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO deleteBooking(Long id) {
        Bookings bookingsFound = bookingsResolver.resolveBookingById(id)
                .orElseThrow(() -> new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));
        bookingsRepository.deleteById(id);

        EventDTO<BookingDeletedEvent> event = new EventDTO<>(
                EventTypeEnum.BOOKING_DELETED,
                bookingEventMapper.toDeletedEvent(bookingsFound),
                LocalDateTime.now(),
                List.of(bookingsFound.getProvider().getId(),bookingsFound.getCustomer().getId()));

        messageProducerBookings.sendEvent(event);

        return bookingMapper.toResponse(bookingsFound);
    }

    public List<BookingsResponseDTO> getBookingsAsProvider() {
        Users user = authUserService.getAuthenticatedUser();

        return bookingsRepository.findByProvider(user)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    public List<BookingsResponseDTO> getBookingsAsCustomer() {
        Users user = authUserService.getAuthenticatedUser();

        return bookingsRepository.findByCustomer(user)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    public void confirmBooking(Long id) {
        Bookings booking = bookingsRepository.findById(id)
                .orElseThrow();

        booking.setStatus(StatusENUM.CONFIRMED);

        bookingsRepository.save(booking);
    }

    public BookingsResponseDTO updateBooking(Long id, BookingsRequestDTO bookingsRequestDTO) {

        Optional<Bookings> booking = bookingsResolver.resolveBookingById(id);

        bookingMapper.updateEntity(bookingsRequestDTO, booking.get(), usersRepository);

        Bookings saved = bookingsRepository.save(booking.get());

        EventDTO<BookingUpdatedEvent> event = new EventDTO<>(
                EventTypeEnum.BOOKING_UPDATED,
                bookingEventMapper.toUpdatedEvent(saved),
                LocalDateTime.now(),
                List.of(saved.getProvider().getId(),saved.getCustomer().getId()));

        messageProducerBookings.sendEvent(event);


        return bookingMapper.toResponse(saved);
    }

    @Cacheable(value = "bookings", key = "'all'")
    public List<BookingsResponseDTO> getAllBookings() {
        List<Bookings> bookings = bookingsRepository.findAll();

        if (bookings.isEmpty()) {
            throw new ApiException("NO BOOKINGS FOUND", HttpStatus.NOT_FOUND);
        }
        return bookings.stream()
                .map(bookingMapper::toResponse)
                .toList();

    }

    // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t) {
        LOG.error("Booking service fallback triggered", t);
        return new ApiException(
                "BOOKING SERVICE TEMPORARILY UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    // saveBooking
    public BookingsResponseDTO handleBookingsEventFailure(BookingsRequestDTO dto, Throwable t) {
        throw serviceUnavailable(t);
    }

    // getBookingById e deleteBooking
    public BookingsResponseDTO handleBookingsEventFailure(Long id, Throwable t) {
        throw serviceUnavailable(t);
    }

    // updateBooking
    public BookingsResponseDTO handleBookingsEventFailure(Long id, BookingsRequestDTO dto, Throwable t) {
        throw serviceUnavailable(t);
    }

    // getAllBookings
    public List<BookingsResponseDTO> handleBookingsEventFailure(Throwable t) {
        throw serviceUnavailable(t);
    }
}
