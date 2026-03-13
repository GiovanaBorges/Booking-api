package com.booking.booking.services.helpers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Bookings;
import com.booking.booking.repositories.BookingsRepository;

@Service
public class BookingsResolver {

    @Autowired
    private BookingsRepository repo;

    public Optional<Bookings> resolveBookingById(Long id) {
        return Optional.of(repo.findById(id)
            .orElseThrow(() -> new ApiException("BOOKING NOT FOUND", HttpStatus.NOT_FOUND)));
}
}
