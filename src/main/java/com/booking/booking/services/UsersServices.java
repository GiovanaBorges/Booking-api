package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.UserMapper;
import com.booking.booking.mappers.events.UserEventMapper;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.helpers.UserFallbackFactory;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UsersServices {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MessageProducerUsers messageProducerUsers;

    @Autowired
    private UserMapper mapperUser;

    @Autowired
    private UserResolver userResolver;

    @Autowired
    private UserEventMapper userEventMapper;

    @Autowired
    private UserFallbackFactory userFallbackFactory;

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

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "handleUserEventFailure")
    @RateLimiter(name = "userRateLimiter")
    @Retry(name = "userRetry", fallbackMethod = "handleUserEventFailure")
    public List<UserResponseDTO> getUsersBySkill(TechSkillsENUM skill) {
        List<Users> usersWithSkill = usersRepository.findUsersBySkill(skill);

        // Transformando para DTO (ex: sem senha, só info públicas)
        return usersWithSkill.stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getDescription(),
                        user.getRoles(), 
                        user.getLinkedinProfile(),
                        user.getGithubProfile(),
                        user.getPortfolioUrl(),
                        user.getPortfolioUrl(), 
                        user.getExperienceYears(),
                        user.getSkills(), 
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
 
    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "handleUserEventFailure")
    @RateLimiter(name = "userRateLimiter")
    @Transactional
    public UserResponseDTO editUser(Long id, UserRequestDTO dto) {
        Users existingUser = userResolver.resolveUserById(id);

        Users updated = usersRepository.save(existingUser);
        userEventMapper.toUpdatedEvent(updated);
        return mapperUser.toResponse(updated);
    }

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "handleUserEventFailure")
    @RateLimiter(name = "userRateLimiter")
    @Retry(name = "userRetry", fallbackMethod = "handleUserEventFailure")
    public UserResponseDTO getUserById(Long id){
        return mapperUser.toResponse(userResolver.resolveUserById(id));
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
        LOG.error("Fallback triggered for createUser jwt={}", jwt, t);
        return userFallbackFactory.createFallbackFromJwt(jwt.getSubject());
    }

    public UserResponseDTO handleUserEventFailure(Long id, Throwable t){
        LOG.error("Fallback triggered for getUserById id={}", id, t);
        return userFallbackFactory.createFallback(id);
    }

    public UserResponseDTO handleUserEventFailure(Long id, UserRequestDTO dto, Throwable t){
        LOG.error("Fallback triggered for editUser dto={}", dto, t);
        return userFallbackFactory.createFallbackForEdit(id);
    }

    public List<UserResponseDTO> handleUserEventFailure(TechSkillsENUM skill, Throwable t){
        LOG.error("Fallback triggered for getUsersBySkill skill={}", skill, t);
        return userFallbackFactory.createFallbackForSkill(skill);
    }
}
