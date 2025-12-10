package com.booking.booking.events.bookingEvents;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingDeletedEvent {
    private Long id;
    private Long providerId;
    private Long customerId;
    private LocalDateTime  startsTs;
    private LocalDateTime  endTs;
    private LocalDateTime eventTs = LocalDateTime.now();
}
