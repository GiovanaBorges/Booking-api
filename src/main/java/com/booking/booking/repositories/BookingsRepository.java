package com.booking.booking.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.booking.booking.models.Bookings;
import java.time.LocalDateTime;



public interface BookingsRepository extends JpaRepository<Bookings,Long>{
    @Query("""
            SELECT b FROM Bookings b
            WHERE b.provider.id = :providerId
            AND(
                (b.startTs <= :endTs AND b.endTs >= :startTs)
            )
            """)
    Optional<Bookings> findConflict(Long providerId,LocalDateTime startsTs,LocalDateTime endTs);

    Optional<Bookings>findById(Long id);
}
