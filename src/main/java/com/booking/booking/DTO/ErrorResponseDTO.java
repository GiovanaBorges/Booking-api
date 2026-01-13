package com.booking.booking.DTO;

import java.time.LocalDateTime;

public record ErrorResponseDTO(int statuscode,String ErrorPhrase,String message,LocalDateTime timestamp) {}
