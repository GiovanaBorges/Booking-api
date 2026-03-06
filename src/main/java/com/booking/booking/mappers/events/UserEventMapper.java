package com.booking.booking.mappers.events;

import org.mapstruct.Mapper;

import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.models.Users;

@Mapper(componentModel = "spring")
public interface UserEventMapper {
    UsersCreatedEvent toCreateEvent(Users model);
}
