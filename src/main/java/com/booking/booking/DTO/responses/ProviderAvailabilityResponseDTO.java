package com.booking.booking.DTO.responses;

import java.time.LocalTime;

import com.booking.booking.models.Users;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ProviderAvailabilityResponseDTO(
    Long id,
    @JsonProperty("day_of_week")
    int dayOfWeek,
    @JsonProperty("start_time")
    LocalTime startTime,
    @JsonProperty("end_time")
    LocalTime endTime,
    Users provider
) {}
