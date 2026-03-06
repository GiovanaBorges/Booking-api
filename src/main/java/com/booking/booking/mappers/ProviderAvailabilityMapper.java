package com.booking.booking.mappers;


import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.models.ProviderAvailability;

public interface ProviderAvailabilityMapper {
    @Mapping(target = "provider", ignore = true)
    ProviderAvailability toEntity(ProviderAvailabilityRequestDTO requestDTO);

    ProviderAvailabilityResponseDTO toResponse(ProviderAvailability model);

    void updateEntity(ProviderAvailabilityRequestDTO requestDTO , @MappingTarget ProviderAvailability model);
}
