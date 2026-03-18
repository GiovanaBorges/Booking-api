package com.booking.booking.DTO.requests;

import java.time.LocalDateTime;
import com.booking.booking.ENUMS.StatusENUM;

import jakarta.persistence.Column;

public record BookingsRequestDTO(
    Long providerId,
    Long customerId,
    LocalDateTime startsTs,
    LocalDateTime endTs,
    StatusENUM status,
    String title,
    String description
) {}
