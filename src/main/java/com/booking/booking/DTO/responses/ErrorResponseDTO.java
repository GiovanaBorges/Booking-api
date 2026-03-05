package com.booking.booking.DTO.responses;

import java.time.LocalDateTime;

public record ErrorResponseDTO(int statuscode,String ErrorPhrase,String message,LocalDateTime timestamp) {}
