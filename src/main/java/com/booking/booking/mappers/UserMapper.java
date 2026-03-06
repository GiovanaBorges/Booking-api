package com.booking.booking.mappers;

import org.mapstruct.Mapper;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.models.Users;

@Mapper(componentModel = "spring")
public interface UserMapper {
    Users toEntity(UserRequestDTO userRequestDTO);
    UserResponseDTO toResponse(Users user);    
}
