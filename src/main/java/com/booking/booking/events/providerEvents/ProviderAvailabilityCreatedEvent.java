package com.booking.booking.events;

import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Builder;

@Builder
public class ProviderAvailabilityCreatedEvent {
    private Long id;
    private int day_of_week; //--1==monday 7==sunday
    private Long providerId;
    private LocalTime  start_time;
    private LocalTime  end_time;
    private LocalDateTime eventTs = LocalDateTime.now();
}
