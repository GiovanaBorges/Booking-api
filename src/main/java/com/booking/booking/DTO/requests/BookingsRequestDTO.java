package com.booking.booking.DTO.requests;

import java.time.LocalDateTime;
import com.booking.booking.ENUMS.StatusENUM;

public record BookingsRequestDTO(
    Long providerId,
    Long customerId,
    LocalDateTime startsTs,
    LocalDateTime endTs,
    StatusENUM status
) {}
