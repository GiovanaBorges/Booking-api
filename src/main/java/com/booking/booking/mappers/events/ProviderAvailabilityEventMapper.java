package com.booking.booking.mappers.events;

import org.mapstruct.Mapper;

import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
import com.booking.booking.models.ProviderAvailability;

@Mapper(componentModel = "spring")
public interface ProviderAvailabilityEventMapper {
    ProviderAvailabilityCreatedEvent toCreateEvent(ProviderAvailability model);
    ProviderAvailabilityUpdatedEvent toUpdatedEvent(ProviderAvailability model);
    ProviderAvailabilityDeletedEvent toDeletedEvent(ProviderAvailability model);
}
