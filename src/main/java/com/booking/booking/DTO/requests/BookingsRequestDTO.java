package com.booking.booking.DTO.requests;

import java.time.LocalDateTime;
import com.booking.booking.ENUMS.StatusENUM;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;

public record BookingsRequestDTO(
    Long providerId,
    Long customerId,
    @JsonProperty("start_ts")
    LocalDateTime startTs,
    @JsonProperty("end_ts")
    LocalDateTime endTs,
    StatusENUM status,
    String title,
    String description
) {}
