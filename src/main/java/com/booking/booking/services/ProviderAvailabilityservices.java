package com.booking.booking.services;

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

import com.booking.booking.DTO.requests.BookingsRequestDTO;
import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.responses.BookingsResponseDTO;
import com.booking.booking.DTO.responses.ProviderAvailabilityResponseDTO;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
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
    
    @Autowired
    private ProviderAvailabilityRepository providerRepository;

    @Autowired
    private MessageProducerProvider messageProducerProvider;

    @Autowired
    private ProviderAvailabilityMapper providerAvailabilityMapper;

    @Autowired
    private ProviderAvailabilityEventMapper providerEventMapper;

    @Autowired
    private UserResolver userResolver;

    @Autowired
    private ProviderAvailabilityResolver providerAvailabilityResolver;
 
    private static final Logger LOG =
    LoggerFactory.getLogger(BookingsServices.class);

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO saveProviderAvailability(ProviderAvailabilityRequestDTO requestDTO){
        
        Users provider = userResolver.resolveUserById(requestDTO.providerId());

        ProviderAvailability providerToBeSaved = providerAvailabilityMapper.toEntity(requestDTO);
        providerToBeSaved.setProvider(provider);
        
        ProviderAvailability result = providerRepository.save(providerToBeSaved);

        // create event for rabbitMQ
        ProviderAvailabilityCreatedEvent event = providerEventMapper.toCreateEvent(result);
            
            // send event to RabbitMQ
            messageProducerProvider.sendProviderCreateEvent(event);

            return providerAvailabilityMapper.toResponse(result);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO deleteProviderById(Long id){
        ProviderAvailability providerFound = providerAvailabilityResolver.resolveProviderById(id);
        providerRepository.deleteById(id);

        // delete event for rabbitMQ
        ProviderAvailabilityDeletedEvent event = providerEventMapper.toDeletedEvent(providerFound);
            
         // send event to RabbitMQ
        messageProducerProvider.sendProviderDeleteEvent(event);

        return providerAvailabilityMapper.toResponse(providerFound);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    @Retry(name = "providerAvailabilityRetry", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @Cacheable(value = "providerAvailability", key = "#id")
    public ProviderAvailabilityResponseDTO findProviderById(Long id){
        ProviderAvailability providerAvailable = providerAvailabilityResolver.resolveProviderById(id);
        
        return providerAvailabilityMapper.toResponse(providerAvailable);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    public ProviderAvailabilityResponseDTO updateProvider(Long id,ProviderAvailabilityRequestDTO requestDTO){
        ProviderAvailability providerFound = providerAvailabilityResolver.resolveProviderById(id);
        
        // updating data
        providerAvailabilityMapper.updateEntity(requestDTO,providerFound);

        ProviderAvailability updated = providerRepository.save(providerFound);

         // delete event for rabbitMQ
        ProviderAvailabilityUpdatedEvent event = providerEventMapper.toUpdatedEvent(updated);
            
        // send event to RabbitMQ
        messageProducerProvider.sendProviderUpdateEvent(event);


        return providerAvailabilityMapper.toResponse(updated);
    }

    @Bulkhead(name = "providerAvailabilityBulkead")
    @CircuitBreaker(name = "providerAvailabilityCircuitBreaker", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @RateLimiter(name = "providerAvailabilityRateLimiter")
    @Retry(name = "providerAvailabilityRetry", fallbackMethod = "handleProviderAvailabilityEventFailure")
    @Cacheable(value = "providerAvailability", key = "'all'")
    public List<ProviderAvailabilityResponseDTO> getAllProvider(){
        List<ProviderAvailability> resultAllProvidersAvailable = providerRepository.findAll();

        if(resultAllProvidersAvailable.isEmpty()){
            throw new ApiException("PROVIDER AVAILABILITY NOT FOUND", HttpStatus.NOT_FOUND);
        }
        return resultAllProvidersAvailable.stream()
            .map(provider -> providerAvailabilityMapper.toResponse(provider))
            .collect(Collectors.toList());
    }

    
    // ============================
    // FALLBACK METHODS
    // ============================

    private RuntimeException serviceUnavailable(Throwable t){
        LOG.error("Provider availability service fallback triggered", t);
        return new ApiException(
            "PROVIDER AVAILABILITY SERVICE TEMPORARILY UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    // saveProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(ProviderAvailabilityRequestDTO dto, Throwable t){
        throw serviceUnavailable(t);
    }

    // getProviderAvailabilityById e deleteProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(Long id, Throwable t){
        throw serviceUnavailable(t);
    }

    // updateProviderAvailability
    public ProviderAvailabilityResponseDTO handleProviderAvailabilityEventFailure(Long id, ProviderAvailabilityRequestDTO dto, Throwable t){
        throw serviceUnavailable(t);
    }

    // getAllProviderAvailability
    public List<ProviderAvailabilityResponseDTO> handleProviderAvailabilityEventFailure(Throwable t){
        throw serviceUnavailable(t);
    }

}
