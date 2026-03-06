package com.booking.booking.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerBookings;

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
        
        
        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND",HttpStatus.NOT_FOUND));

        Users customer = usersRepository.findById(requestDTO.customerId())
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));
        
        Bookings booking = bookingMapper.toEntity(requestDTO);
        booking.setProvider(provider);
        booking.setCustomer(customer);
        
        Bookings bookingSaved = bookingsRepository.save(booking);

        BookingCreatedEvent bookingCreatedEvent = bookingEventMapper.toCreatedEvent(bookingSaved);

        messageProducerBookings.sendBookingCreateEvent(bookingCreatedEvent);

        return bookingMapper.toResponse(bookingSaved);
    }

    @Cacheable(value = "bookings", key = "#id")
    public BookingsResponseDTO getBookingById(Long id){
        Optional<Bookings> bookingsFound = bookingsRepository.findById(id);
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        return bookingMapper.toResponse(bookingsFound.get());
    }

    
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO deleteBooking(Long id){
        Optional<Bookings> bookingsFound = bookingsRepository.findById(id);
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        bookingsRepository.deleteById(id);

        BookingDeletedEvent bookingDeletedEvent = bookingEventMapper.toDeletedEvent(bookingsFound.get());

        messageProducerBookings.sendBookingDeleteEvent(bookingDeletedEvent);
            
        return bookingMapper.toResponse(bookingsFound.get());
    }

    public BookingsResponseDTO updateBooking(Long id,BookingsRequestDTO bookingsRequestDTO){
        
        Bookings booking = bookingsRepository.findById(id)
        .orElseThrow(() -> new ApiException(
            "BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));

        Users provider = usersRepository.findById(bookingsRequestDTO.providerId())
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND",HttpStatus.NOT_FOUND));

        Users customer = usersRepository.findById(bookingsRequestDTO.customerId())
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));

        bookingMapper.updateEntity(bookingsRequestDTO,booking);
        booking.setProvider(provider);
        booking.setCustomer(customer);
        
        Bookings saved = bookingsRepository.save(booking);

        BookingUpdatedEvent event = bookingEventMapper.toUpdatedEvent(saved);

        messageProducerBookings.sendBookingUpdateEvent(event);

        return bookingMapper.toResponse(saved);
    }

    @Cacheable(value = "bookings", key = "'all'")
    public List<BookingsResponseDTO> getAllBookings(){
        List<Bookings> bookingsFound = bookingsRepository.findAll();
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        return bookingsFound.stream()
            .map(booking -> bookingMapper.toResponse(booking))
            .toList();

    }   
}
