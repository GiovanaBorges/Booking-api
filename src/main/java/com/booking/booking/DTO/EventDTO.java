package com.booking.booking.DTO;

import java.time.LocalDateTime;
import java.util.List;

import com.booking.booking.ENUMS.EventTypeEnum;

public record EventDTO<T>(
    EventTypeEnum type,
    T data,
    LocalDateTime createdAt , 
    List<Long> recipients
) {
}
