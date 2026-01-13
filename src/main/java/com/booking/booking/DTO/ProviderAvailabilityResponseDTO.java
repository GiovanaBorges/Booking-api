package com.booking.booking.DTO;

import java.time.LocalTime;

import com.booking.booking.models.Users;

public record ProviderAvailabilityResponseDTO(
    Long id,
    int day_of_week,
    LocalTime start_time,
    LocalTime end_time,
    Users provider
) {}
