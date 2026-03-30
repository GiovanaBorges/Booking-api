package com.booking.booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booking.booking.ENUMS.StatusENUM;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;

import java.time.LocalDateTime;



public interface BookingsRepository extends JpaRepository<Bookings,Long>{
    @Query("""
            SELECT b FROM Bookings b
            WHERE b.provider.id = :providerId
            AND(
                (b.startTs <= :endTs AND b.endTs >= :startTs)
            )
            """)
    Optional<Bookings> findConflict( 
        @Param("providerId") Long providerId,
        @Param("startTs") LocalDateTime startTs,
        @Param("endTs") LocalDateTime endTs
    );

    Optional<Bookings>findById(Long id);

    List<Bookings> findByProvider(Users provider);
    List<Bookings> findByCustomer(Users customer);

    List<Bookings> findByProviderAndStatus(Users provider, StatusENUM status);
}
