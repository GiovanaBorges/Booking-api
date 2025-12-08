package com.booking.booking.events.providerEvents;

import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderAvailabilityDeletedEvent {
    private Long id;
    private Long providerId;
    private int day_of_week;
    private LocalTime start_time;
    private LocalTime end_time;

    private LocalDateTime eventTs;
}
