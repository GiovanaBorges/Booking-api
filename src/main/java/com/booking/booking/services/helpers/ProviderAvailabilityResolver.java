package com.booking.booking.services.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.repositories.ProviderAvailabilityRepository;

@Service
public class ProviderAvailabilityResolver {
    @Autowired
    private ProviderAvailabilityRepository repo;

    public ProviderAvailability resolveProviderById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ApiException("PROVIDER AVAILABILITY NOT FOUND", HttpStatus.NOT_FOUND));
}
}
