package com.booking.booking.events.bookingEvents;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BookingCreatedEvent {
    private Long id;
    private Long providerId;
    private Long customerId;
    @JsonProperty("start_ts")
    private LocalDateTime  startsTs;
    @JsonProperty("end_ts")
    private LocalDateTime  endTs;
    private LocalDateTime eventTs = LocalDateTime.now();
}
