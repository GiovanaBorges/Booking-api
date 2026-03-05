package com.booking.booking.DTO.responses;

import java.time.LocalDateTime;

import com.booking.booking.ENUMS.StatusENUM;

public record BookingsResponseDTO(
    Long id,
    Long provider,
    Long customer,
    LocalDateTime startsTs,
    LocalDateTime endTs,
    StatusENUM status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
    ) {}