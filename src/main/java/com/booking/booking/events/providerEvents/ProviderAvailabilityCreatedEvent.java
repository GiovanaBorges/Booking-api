package com.booking.booking.events.providerEvents;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProviderAvailabilityCreatedEvent {
    private Long id;
    @JsonProperty("day_of_week")
    private int dayOfWeek; //--1==monday 7==sunday
    private Long providerId;
    @JsonProperty("start_time")
    private LocalTime  startTime;
    @JsonProperty("end_time")
    private LocalTime  endTime;
    private LocalDateTime eventTs = LocalDateTime.now();
}
