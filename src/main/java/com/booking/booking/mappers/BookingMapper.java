package com.booking.booking.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.http.HttpStatus;

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;

@Mapper(componentModel = "spring")
public interface BookingMapper {
      // → Request DTO (Long IDs) para Entidade (Users)
    @Mapping(target = "provider", source = "providerId", qualifiedByName = "mapProvider")
    @Mapping(target = "customer", source = "customerId", qualifiedByName = "mapCustomer")
    Bookings toEntity(BookingsRequestDTO requestDTO, @Context UsersRepository usersRepository);

    // → Entidade (Users) para Response DTO (Long IDs)
    @Mapping(target = "provider",source = "provider", qualifiedByName = "mapToId")
    @Mapping(target = "customer", source = "customer", qualifiedByName = "mapToId")
    BookingsResponseDTO toResponse(Bookings model);

    @Mapping(target = "provider", source = "providerId", qualifiedByName = "mapProvider")
    @Mapping(target = "customer", source = "customerId", qualifiedByName = "mapCustomer")
    void updateEntity(BookingsRequestDTO requestDTO, @MappingTarget Bookings model, @Context UsersRepository usersRepository);

    // ======================
    // Métodos auxiliares
    // ======================

    @Named("mapProvider")
    default Users mapProvider(Long id, @Context UsersRepository repo){
        if (id == null) return null;
        return repo.findById(id)
            .orElseThrow(() -> new ApiException("PROVIDER NOT FOUND", HttpStatus.NOT_FOUND));
    }

    @Named("mapCustomer")
    default Users mapCustomer(Long id, @Context UsersRepository repo){
        if (id == null) return null;
        return repo.findById(id)
            .orElseThrow(() -> new ApiException("CUSTOMER NOT FOUND", HttpStatus.NOT_FOUND));
    }

    @Named("mapToId")
    default Long mapToId(Users user){
        return user != null ? user.getId() : null;
    }
}
