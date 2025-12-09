package com.booking.booking.events.usersEvents;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsersDeletedEvent {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String roles;
    private LocalDateTime createdAt;
    private LocalDateTime eventTs = LocalDateTime.now();
}
