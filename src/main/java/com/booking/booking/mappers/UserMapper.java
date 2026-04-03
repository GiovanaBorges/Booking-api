package com.booking.booking.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.models.Users;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "createdAt", ignore = true)
    Users toEntity(UserRequestDTO userRequestDTO);
    UserResponseDTO toResponse(Users user);    
    void updateEntity(UserRequestDTO requestDTO , @MappingTarget Users model);
}
