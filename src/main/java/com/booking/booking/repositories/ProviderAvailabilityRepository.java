package com.booking.booking.repositories;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability,Long>{
    Optional<ProviderAvailability> findById(Long id);
    
    List<ProviderAvailability> findByProvider(Users provider);

    List<ProviderAvailability> findByProviderAndDayOfWeek(Users provider, int dayOfWeek);
}
