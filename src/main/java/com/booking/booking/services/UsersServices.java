package com.booking.booking.services;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.UserMapper;
import com.booking.booking.mappers.events.UserEventMapper;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class UsersServices {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MessageProducerUsers messageProducerUsers;

    @Autowired
    private UserMapper mapperUser;

    @Autowired
    private UserEventMapper userEventMapper;

    private static final Logger LOG =
    LoggerFactory.getLogger(BookingsServices.class);

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "handleUserEventFailure")
    @RateLimiter(name = "userRateLimiter")
    public UserResponseDTO createOrGet(Jwt jwt) {

        String keycloakId = jwt.getSubject();
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");

        RolesENUM role = RolesENUM.ADMIN;
        Users user = usersRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {

                    Users newUser = Users.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .name(name)
                            .roles(role)
                            .createdAt(LocalDateTime.now())
                            .build();

                    Users savedUser = usersRepository.save(newUser);

                    messageProducerUsers.sendUsersCreateEvent(userEventMapper.toCreateEvent(savedUser));

                    return savedUser;
                });

        return mapperUser.toResponse(user);
    }

     // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t){
        LOG.error("User service fallback triggered", t);
        return new ApiException(
            "USER SERVICE TEMPORARILY UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    public UserResponseDTO handleUserEventFailure(Jwt jwt, Throwable t){
        throw serviceUnavailable(t);
    }
}
