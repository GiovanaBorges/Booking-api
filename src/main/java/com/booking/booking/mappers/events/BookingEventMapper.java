package com.booking.booking.mappers.events;

import org.mapstruct.Mapper;

import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;
import com.booking.booking.models.Bookings;

@Mapper(componentModel = "spring")
public interface BookingEventMapper {
    BookingCreatedEvent toCreatedEvent(Bookings model);
    BookingUpdatedEvent toUpdatedEvent(Bookings model);
    BookingDeletedEvent toDeletedEvent(Bookings model);
}
