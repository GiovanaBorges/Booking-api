package com.booking.booking.services;

import com.booking.booking.repositories.BookingsRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.EventDTO;
import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.ENUMS.EventTypeEnum;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.mappers.ProviderAvailabilityMapper;
import com.booking.booking.mappers.events.ProviderAvailabilityEventMapper;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.services.helpers.ProviderAvailabilityResolver;
import com.booking.booking.services.helpers.UserResolver;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ProviderAvailabilityservices {

    private final UserResolver userResolver;
    private final ProviderAvailabilityRepository providerRepository;
    private final MessageProducerProvider messageProducerProvider;
    private final ProviderAvailabilityMapper providerAvailabilityMapper;
    private final ProviderAvailabilityEventMapper providerEventMapper;
    private final ProviderAvailabilityResolver providerAvailabilityResolver;
    private final AuthenticatedUserService authUserService;

    private static final Logger LOG = LoggerFactory.getLogger(BookingsServices.class);

    ProviderAvailabilityservices(
            UserResolver userResolver,
            ProviderAvailabilityRepository providerAvailabilityRepository,
            MessageProducerProvider messageProducerProvider,
            ProviderAvailabilityMapper providerAvailabilityMapper,
            ProviderAvailabilityEventMapper providerAvailabilityEventMapper,
            ProviderAvailabilityResolver providerAvailabilityResolver,
            AuthenticatedUserService authenticatedUserService) {
        this.userResolver = userResolver;
        this.providerRepository = providerAvailabilityRepository;
        this.messageProducerProvider = messageProducerProvider;
        this.providerAvailabilityMapper = providerAvailabilityMapper;
        this.providerEventMapper = providerAvailabilityEventMapper;
        this.providerAvailabilityResolver = providerAvailabilityResolver;
        this.authUserService = authenticatedUserService;
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO saveProviderAvailability(ProviderAvailabilityRequestDTO requestDTO) {

        Users provider = userResolver.getAuthenticatedUser();

        ProviderAvailability providerToBeSaved = providerAvailabilityMapper.toEntity(requestDTO);
        providerToBeSaved.setProvider(provider);

        ProviderAvailability result = providerRepository.save(providerToBeSaved);

        EventDTO<ProviderAvailabilityCreatedEvent> event = new EventDTO<>(
                EventTypeEnum.PROVIDER_CREATED,
                providerEventMapper.toCreateEvent(result),
                LocalDateTime.now(),
                List.of(result.getProvider().getId()));

        messageProducerProvider.sendEvent(event);

        return providerAvailabilityMapper.toResponse(result);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker") 
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO deleteProviderById(Long id) {

        Users loggedUser = userResolver.getAuthenticatedUser();

        ProviderAvailability providerFound = providerAvailabilityResolver.resolveProviderById(id);

        if (!providerFound.getProvider().getId().equals(loggedUser.getId())) {
            throw new ApiException("FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        providerRepository.deleteById(id);

       EventDTO<ProviderAvailabilityDeletedEvent> event = new EventDTO<>(
                EventTypeEnum.PROVIDER_DELETED,
                providerEventMapper.toDeletedEvent(providerFound),
                LocalDateTime.now(),
                List.of(providerFound.getProvider().getId()));

        messageProducerProvider.sendEvent(event);

        return providerAvailabilityMapper.toResponse(providerFound);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    @Retry(name = "providerAvailabilityRetry")
    @Cacheable(value = "providerAvailability", key = "#id")
    public ProviderAvailabilityResponseDTO findProviderById(Long id) {
        ProviderAvailability providerAvailable = providerAvailabilityResolver.resolveProviderById(id);

        return providerAvailabilityMapper.toResponse(providerAvailable);
    }

    public List<ProviderAvailabilityResponseDTO> getAvailabilityByDate(Long providerId, String date) {

        LocalDate parsedDate = LocalDate.parse(date);

        int dayOfWeek = parsedDate.getDayOfWeek().getValue();
        // Monday=1 ... Sunday=7

        // seu sistema: 0=domingo
        dayOfWeek = dayOfWeek % 7;

        Users provider = userResolver.resolveUserById(providerId);

        System.out.println("PROVIDER ID: " + provider.getId());

        return providerRepository.findByProviderAndDayOfWeek(provider, dayOfWeek)
                .stream()
                .map(providerAvailabilityMapper::toResponse)
                .toList();
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO updateProvider(Long id, ProviderAvailabilityRequestDTO requestDTO) {

        Users loggedUser = userResolver.getAuthenticatedUser();

        ProviderAvailability providerFound = providerAvailabilityResolver.resolveProviderById(id);

        if (!providerFound.getProvider().getId().equals(loggedUser.getId())) {
            throw new ApiException("FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        // updating data
        providerAvailabilityMapper.updateEntity(requestDTO, providerFound);

        ProviderAvailability updated = providerRepository.save(providerFound);

       EventDTO<ProviderAvailabilityUpdatedEvent> event = new EventDTO<>(
                EventTypeEnum.PROVIDER_UPDATED,
                providerEventMapper.toUpdatedEvent(updated),
                LocalDateTime.now(),
                List.of(updated.getProvider().getId()));

        messageProducerProvider.sendEvent(event);

        return providerAvailabilityMapper.toResponse(updated);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    @Retry(name = "providerAvailabilityRetry")
    @Cacheable(value = "providerAvailability", key = "'all'")
    public List<ProviderAvailabilityResponseDTO> getAllProvider() {
        List<ProviderAvailability> resultAllProvidersAvailable = providerRepository.findAll();

        if (resultAllProvidersAvailable.isEmpty()) {
            throw new ApiException("PROVIDER AVAILABILITY NOT FOUND", HttpStatus.NOT_FOUND);
        }
        return resultAllProvidersAvailable.stream()
                .map(provider -> providerAvailabilityMapper.toResponse(provider))
                .collect(Collectors.toList());
    }

    public List<ProviderAvailabilityResponseDTO> getMyAvailabilities() {

        Users user = authUserService.getAuthenticatedUser();
        System.out.println("PROVIDER ID: " + user.getId());

        return providerRepository.findByProvider(user)
                .stream()
                .map(provider -> providerAvailabilityMapper.toResponse(provider))
                .collect(Collectors.toList());

    }

    // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t) {
        LOG.error("Provider availability service fallback triggered", t);
        return new ApiException(
                "PROVIDER AVAILABILITY SERVICE TEMPORARILY UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    // saveProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(ProviderAvailabilityRequestDTO dto,
            Throwable t) {
        throw serviceUnavailable(t);
    }

    // getProviderAvailabilityById e deleteProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(Long id, Throwable t) {
        throw serviceUnavailable(t);
    }

    // updateProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(Long id,
            ProviderAvailabilityRequestDTO dto, Throwable t) {
        throw serviceUnavailable(t);
    }

    // getAllProviderAvailability
    public List<ProviderAvailabilityResponseDTO> handleProviderAvailabilityEventFailure(Throwable t) {
        throw serviceUnavailable(t);
    }

}
