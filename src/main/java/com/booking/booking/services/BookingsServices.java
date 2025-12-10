package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.BookingsRequestDTO;
import com.booking.booking.DTO.BookingsResponseDTO;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
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

    public BookingsResponseDTO saveBooking(BookingsRequestDTO requestDTO){

        Optional<Bookings> conflict = bookingsRepository.findConflict(
            requestDTO.providerId(),
            requestDTO.startsTs(),
            requestDTO.endTs()
        );

        if(conflict.isPresent()){
            throw new ApiException("THIS TIME SLOT IS ALREADY BOOKED", HttpStatus.CONFLICT);
        }
        
        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND",HttpStatus.NOT_FOUND));

        Users customer = usersRepository.findById(requestDTO.customerId())
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));
        
        Bookings booking = Bookings.builder()
            .provider(provider)
            .customer(customer)
            .startsTs(requestDTO.startsTs())
            .endTs(requestDTO.endTs())
            .status(requestDTO.status())
            .build();
        
        Bookings bookingSaved = bookingsRepository.save(booking);

        BookingCreatedEvent bookingCreatedEvent = BookingCreatedEvent.builder()
            .id(bookingSaved.getId())
            .customerId(bookingSaved.getCustomer().getId())
            .endTs(bookingSaved.getEndTs())
            .startsTs(bookingSaved.getStartsTs())
            .providerId(bookingSaved.getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();

        messageProducerBookings.sendBookingCreateEvent(bookingCreatedEvent);

        return new BookingsResponseDTO(
            bookingSaved.getId(),
            bookingSaved.getProvider().getId(),
            bookingSaved.getCustomer().getId(),
            bookingSaved.getStartsTs(),
            bookingSaved.getEndTs(),
            bookingSaved.getStatus(),
            bookingSaved.getCreatedAt(),
            bookingSaved.getUpdatedAt()
        );
    }

    public BookingsResponseDTO getBookingById(Long id){
        Optional<Bookings> bookingsFound = bookingsRepository.findById(id);
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        return new BookingsResponseDTO(
            bookingsFound.get().getId(),
            bookingsFound.get().getProvider().getId(),
            bookingsFound.get().getCustomer().getId(),
            bookingsFound.get().getStartsTs(),
            bookingsFound.get().getEndTs(),
            bookingsFound.get().getStatus(),
            bookingsFound.get().getCreatedAt(),
            bookingsFound.get().getUpdatedAt()
        );
    }

    public BookingsResponseDTO deleteBooking(Long id){
        Optional<Bookings> bookingsFound = bookingsRepository.findById(id);
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        bookingsRepository.deleteById(id);

        BookingDeletedEvent bookingDeletedEvent = BookingDeletedEvent.builder()
            .id(bookingsFound.get().getId())
            .customerId(bookingsFound.get().getCustomer().getId())
            .endTs(bookingsFound.get().getEndTs())
            .startsTs(bookingsFound.get().getStartsTs())
            .providerId(bookingsFound.get().getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();

        messageProducerBookings.sendBookingDeleteEvent(bookingDeletedEvent);
            
        return new BookingsResponseDTO(
            bookingsFound.get().getId(),
            bookingsFound.get().getProvider().getId(),
            bookingsFound.get().getCustomer().getId(),
            bookingsFound.get().getStartsTs(),
            bookingsFound.get().getEndTs(),
            bookingsFound.get().getStatus(),
            bookingsFound.get().getCreatedAt(),
            bookingsFound.get().getUpdatedAt()
        );
    }

    public BookingsResponseDTO updateBooking(Long id,BookingsRequestDTO bookingsRequestDTO){
        if(bookingsRepository.findById(id).isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        Users provider = usersRepository.findById(bookingsRequestDTO.providerId())
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND",HttpStatus.NOT_FOUND));

        Users customer = usersRepository.findById(bookingsRequestDTO.customerId())
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));
        
        Optional<Bookings> bookingFound = bookingsRepository.findById(id);

        Bookings booking = Bookings.builder()
            .customer(customer)
            .status(bookingFound.get().getStatus())
            .provider(provider)
            .endTs(bookingFound.get().getEndTs())
            .startsTs(bookingFound.get().getStartsTs())
            .build();

        Bookings bookingsUpdated = bookingsRepository.save(booking);

        BookingUpdatedEvent bookingUpdatedEvent = BookingUpdatedEvent.builder()
            .id(bookingsUpdated.getId())
            .customerId(bookingsUpdated.getCustomer().getId())
            .endTs(bookingsUpdated.getEndTs())
            .startsTs(bookingsUpdated.getStartsTs())
            .providerId(bookingsUpdated.getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();

        messageProducerBookings.sendBookingUpdateEvent(bookingUpdatedEvent);

        return new BookingsResponseDTO(
            bookingsUpdated.getId(),
            bookingsUpdated.getProvider().getId(),
            bookingsUpdated.getCustomer().getId(),
            bookingsUpdated.getStartsTs(),
            bookingsUpdated.getEndTs(),
            bookingsUpdated.getStatus(),
            bookingsUpdated.getCreatedAt(),
            bookingsUpdated.getUpdatedAt()
        );
    }

    public List<BookingsResponseDTO> getAllBookings(){
        List<Bookings> bookingsFound = bookingsRepository.findAll();
        if(bookingsFound.isEmpty()){
            throw new ApiException("BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND);
        }

        return bookingsFound.stream()
            .map(b -> new BookingsResponseDTO(
                    b.getId(),
                    b.getProvider().getId(),
                    b.getCustomer().getId(),
                    b.getStartsTs(),
                    b.getEndTs(),
                    b.getStatus(),
                    b.getCreatedAt(),
                    b.getUpdatedAt()))
                    .collect(Collectors.toList());

    }   
}
