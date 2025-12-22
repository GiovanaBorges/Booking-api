package com.booking.booking.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Autowired
    private LockService lockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    
    @CacheEvict(value = "bookings", allEntries = true)
    public BookingsResponseDTO saveBooking(BookingsRequestDTO requestDTO,String idempotencyKey){

        String idempotencyRedisKey = "idempotency:" +idempotencyKey;

        String existing = redisTemplate.opsForValue().get(idempotencyRedisKey);
        if(existing != null){
            throw new ApiException("IDEMPOTENCY REPLAY", HttpStatus.CONFLICT);
        }

        String lockKey = String.format(
            "lock:booking:%d:%s:%s",
            requestDTO.providerId(),
            requestDTO.startsTs(),
            requestDTO.endTs()
        );

        boolean locked = lockService.acquireLock(lockKey, Duration.ofSeconds(10));
        if(!locked){
            throw new ApiException("LOCK NOT ACQUIRED", HttpStatus.LOCKED);
        }
        
        try{
            Optional<Bookings> conflict = bookingsRepository.findConflict(
                requestDTO.providerId(), 
                requestDTO.startsTs(),
                requestDTO.endTs()
            );

            if(conflict.isPresent()){
                throw new ApiException("THIS TIME SLOT IS ALREADY BOOKED",
                 HttpStatus.CONFLICT);
            }
        

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

        redisTemplate.opsForValue()
            .set(idempotencyRedisKey, 
                bookingSaved.getId().toString(),
                Duration.ofHours(24)
            );

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

    } finally{
        lockService.releaseLock(lockKey);
    }
    }

    @Cacheable(value = "bookings", key = "#id")
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

    
    @CacheEvict(value = "bookings", allEntries = true)
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
        
        Bookings booking = bookingsRepository.findById(id)
        .orElseThrow(() -> new ApiException(
            "BOOKINGS NOT FOUND", HttpStatus.NOT_FOUND));

        Users provider = usersRepository.findById(bookingsRequestDTO.providerId())
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND",HttpStatus.NOT_FOUND));

        Users customer = usersRepository.findById(bookingsRequestDTO.customerId())
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));

        booking.setProvider(provider);
        booking.setCustomer(customer);
        booking.setStartsTs(bookingsRequestDTO.startsTs());
        booking.setEndTs(bookingsRequestDTO.endTs());
        booking.setStatus(bookingsRequestDTO.status());

        Bookings saved = bookingsRepository.save(booking);

         BookingUpdatedEvent event = BookingUpdatedEvent.builder()
            .id(saved.getId())
            .providerId(saved.getProvider().getId())
            .customerId(saved.getCustomer().getId())
            .startsTs(saved.getStartsTs())
            .endTs(saved.getEndTs())
            .eventTs(LocalDateTime.now())
            .build();


        messageProducerBookings.sendBookingUpdateEvent(event);

        return new BookingsResponseDTO(
            saved.getId(),
            saved.getProvider().getId(),
            saved.getCustomer().getId(),
            saved.getStartsTs(),
            saved.getEndTs(),
            saved.getStatus(),
            saved.getCreatedAt(),
            saved.getUpdatedAt()
        );
    }

    @Cacheable(value = "bookings", key = "'all'")
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
