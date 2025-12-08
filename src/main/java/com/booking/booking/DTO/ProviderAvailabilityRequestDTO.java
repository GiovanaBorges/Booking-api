package com.booking.booking.DTO;

import java.time.LocalTime;


public record ProviderAvailabilityRequestDTO(
        int day_of_week,
        LocalTime startTime,
        LocalTime end_time,
        Long providerId
    ){}

