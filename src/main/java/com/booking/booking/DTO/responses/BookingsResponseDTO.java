package com.booking.booking.DTO.responses;

import java.time.LocalDateTime;

import com.booking.booking.ENUMS.StatusENUM;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BookingsResponseDTO(
    Long id,
    Long provider,
    Long customer,
    @JsonProperty("start_ts")
    LocalDateTime startTs,
    @JsonProperty("end_ts")
    LocalDateTime endTs,
    StatusENUM status,
    String title,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
    ) {}