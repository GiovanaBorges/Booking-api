package com.booking.booking.mappers;

import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.models.Bookings;

public interface BookingMapper {
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "customer", ignore = true)
    Bookings toEntity(BookingsRequestDTO requestDTO);
    
    BookingsResponseDTO toResponse(Bookings model);
    void updateEntity(BookingsRequestDTO requestDTO, @MappingTarget Bookings model);
}
