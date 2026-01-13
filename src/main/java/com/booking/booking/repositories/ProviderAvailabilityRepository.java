package com.booking.booking.repositories;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.ProviderAvailability;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability,Long>{
    Optional<ProviderAvailability> findById(Long id);
}
