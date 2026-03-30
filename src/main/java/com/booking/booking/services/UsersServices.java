package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.booking.DTO.EventDTO;
import com.booking.booking.DTO.requests.UserRequestDTO;
import com.booking.booking.DTO.responses.UserResponseDTO;
import com.booking.booking.ENUMS.EventTypeEnum;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.events.usersEvents.UsersUpdatedEvent;
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
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class UsersServices {

    private final UsersRepository usersRepository;
    private final MessageProducerUsers messageProducerUsers;
    private final UserMapper mapperUser;
    private final UserResolver userResolver;
    private final UserEventMapper userEventMapper;
    private final UserFallbackFactory userFallbackFactory;
    private final KeycloakService keycloakService;

    private static final Logger LOG = LoggerFactory.getLogger(BookingsServices.class);

    @Bulkhead(name = "userBulkhead")
    @CircuitBreaker(name = "userCircuitBreaker")
    @RateLimiter(name = "userRateLimiter")
    @Transactional
    public Users createOrGet(Jwt jwt) {

        String keycloakId = jwt.getSubject();
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");
        String accountType = jwt.getClaim("account_type");

        Users user = usersRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    System.out.println("NAO TEM CONTA AINDA");

                    return usersRepository.findByEmail(email)
                            .map(existingUser -> {
                                existingUser.setKeycloakId(keycloakId);
                                return usersRepository.save(existingUser);
                            })
                            .orElseGet(() -> {

                                // usa o token
                                RolesENUM role = "provider".equals(accountType)
                                        ? RolesENUM.PROVIDER
                                        : RolesENUM.CLIENT;

                                Users newUser = Users.builder()
                                        .keycloakId(keycloakId)
                                        .email(email)
                                        .name(name)
                                        .roles(role)
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                return usersRepository.save(newUser);
                            });
                });

        RolesENUM roleFromDb = user.getRoles();

        final String groupName;
        final String roleName;

        if (roleFromDb == RolesENUM.PROVIDER) {
            groupName = "providers";
            roleName = "PROVIDER";
        } else {
            groupName = "clients";
            roleName = "CLIENT";
        }

        keycloakService.assignGroup(keycloakId, groupName);
        keycloakService.assignRoleToUser(keycloakId, roleName);

        return user;
    }

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker")
    @RateLimiter(name = "userRateLimiter")
    @Retry(name = "userRetry")
    public List<UserResponseDTO> getUsersBySkill(TechSkillsENUM skill) {
        List<Users> usersWithSkill = usersRepository.findUsersBySkill(skill);

        // Transformando para DTO exexemplo: sem senha, só info públicas
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
                        user.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker")
    @RateLimiter(name = "userRateLimiter")
    @Transactional
    public UserResponseDTO editUser(UserRequestDTO dto, Jwt jwt) {
        String keycloakId = jwt.getSubject();

        Users user = usersRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ApiException("USER NOT FOUND", HttpStatus.NOT_FOUND));

        mapperUser.updateEntity(dto, user);

        Users updated = usersRepository.save(user);

        EventDTO<UsersUpdatedEvent> event = new EventDTO<>(
                EventTypeEnum.USER_UPDATED,
                userEventMapper.toUpdatedEvent(updated),
                LocalDateTime.now(),
                List.of(updated.getId()));

        messageProducerUsers.sendEvent(event);

        return mapperUser.toResponse(updated);
    }

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker")
    @RateLimiter(name = "userRateLimiter")
    @Retry(name = "userRetry")
    public UserResponseDTO getUserById(Long id) {
        return mapperUser.toResponse(userResolver.resolveUserById(id));
    }

    @Bulkhead(name = "userBulkead")
    @CircuitBreaker(name = "userCircuitBreaker")
    @RateLimiter(name = "userRateLimiter")
    @Retry(name = "userRetry")
    public List<UserResponseDTO> getAllProviders() {

        List<Users> providers = usersRepository.findByRoles(RolesENUM.PROVIDER);

        if (providers.isEmpty()) {
            throw new ApiException("NO PROVIDERS FOUND", HttpStatus.NOT_FOUND);
        }

        return providers.stream()
                .map(mapperUser::toResponse)
                .collect(Collectors.toList());
    }

    // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t) {
        LOG.error("User service fallback triggered", t);
        return new ApiException(
                "USER SERVICE TEMPORARILY UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public UserResponseDTO handleUserEventFailure(Jwt jwt, Throwable t) {
        LOG.error("Fallback triggered for createUser jwt={}", jwt, t);
        return userFallbackFactory.createFallbackFromJwt(jwt.getSubject());
    }

    public UserResponseDTO handleUserEventFailure(Long id, Throwable t) {
        LOG.error("Fallback triggered for getUserById id={}", id, t);
        return userFallbackFactory.createFallback(id);
    }

    public List<UserResponseDTO> handleUserEventFailure(Throwable t) {
        LOG.error("Fallback triggered for getAllUsers", t);
        return userFallbackFactory.createFallbackForGetAllProviders();
    }

    public UserResponseDTO handleUserEventFailure(Long id, UserRequestDTO dto, Throwable t) {
        LOG.error("Fallback triggered for editUser dto={}", dto, t);
        return userFallbackFactory.createFallbackForEdit(id);
    }

    public List<UserResponseDTO> handleUserEventFailure(TechSkillsENUM skill, Throwable t) {
        LOG.error("Fallback triggered for getUsersBySkill skill={}", skill, t);
        return userFallbackFactory.createFallbackForSkill(skill);
    }
}
