package com.booking.booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.Bookings;

public interface BookingsRepository extends JpaRepository<Bookings,Long>{
    
}
