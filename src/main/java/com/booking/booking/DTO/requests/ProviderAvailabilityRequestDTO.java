package com.booking.booking.DTO.requests;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;


public record ProviderAvailabilityRequestDTO(
        @JsonProperty("day_of_week")    
        int dayOfWeek,
    
        @JsonProperty("start_time")
        LocalTime startTime,

        @JsonProperty("end_time")
        LocalTime endTime
    ){}

