package com.booking.booking.events.providerEvents;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderAvailabilityUpdatedEvent {
    private Long id;
    private Long providerId;
    @JsonProperty("day_of_week")
    private int dayOfWeek;
    @JsonProperty("start_time")
    private LocalTime startTime;
    @JsonProperty("end_time")
    private LocalTime endTime;
    private LocalDateTime eventTs;
}
