package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.requests.ProviderAvailabilityRequestDTO;
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
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

@Service
public class ProviderAvailabilityservices {
    
    @Autowired
    private ProviderAvailabilityRepository providerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MessageProducerProvider messageProducerProvider;

    @Autowired
    private ProviderAvailabilityMapper providerAvailabilityMapper;

    @Autowired
    private ProviderAvailabilityEventMapper providerEventMapper;
   
    public ProviderAvailabilityResponseDTO saveProviderAvailability(ProviderAvailabilityRequestDTO requestDTO){
        
        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("Provider availability not found", HttpStatus.NOT_FOUND));

        ProviderAvailability providerToBeSaved = providerAvailabilityMapper.toEntity(requestDTO);
        providerToBeSaved.setProvider(provider);
        
        ProviderAvailability result = providerRepository.save(providerToBeSaved);

        // create event for rabbitMQ
        ProviderAvailabilityCreatedEvent event = providerEventMapper.toCreateEvent(result);
            
            // send event to RabbitMQ
            messageProducerProvider.sendProviderCreateEvent(event);

            return providerAvailabilityMapper.toResponse(result);
    }

    public ProviderAvailabilityResponseDTO deleteProviderById(Long id){
        ProviderAvailability providerFound = providerRepository.findById(id)
            .orElseThrow(() -> new ApiException("Provider availability not found", HttpStatus.NOT_FOUND));

        providerRepository.deleteById(id);


        // delete event for rabbitMQ
        ProviderAvailabilityDeletedEvent event = providerEventMapper.toDeletedEvent(providerFound.get());
            
         // send event to RabbitMQ
        messageProducerProvider.sendProviderDeleteEvent(event);

        return providerAvailabilityMapper.toResponse(providerFound);
    }

    public ProviderAvailabilityResponseDTO findProviderById(Long id){
        Optional<ProviderAvailability> providerAvailable = providerRepository.findById(id);
        if(providerAvailable.isEmpty()){
            throw new ApiException("Provider availability not found",HttpStatus.NOT_FOUND);
        }
        return providerAvailabilityMapper.toResponse(providerAvailable.get());
    }

    public ProviderAvailabilityResponseDTO updateProvider(Long id,ProviderAvailabilityRequestDTO requestDTO){
        ProviderAvailability providerFound = providerRepository.findById(id)
            .orElseThrow(() -> new ApiException("Provider availability not found", HttpStatus.NOT_FOUND));

        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        // updating data
        providerAvailabilityMapper.updateEntity(requestDTO,providerFound);
        providerFound.setProvider(provider);

        ProviderAvailability updated = providerRepository.save(providerFound);

         // delete event for rabbitMQ
        ProviderAvailabilityUpdatedEvent event = providerEventMapper.toUpdatedEvent(updated);
            
        // send event to RabbitMQ
        messageProducerProvider.sendProviderUpdateEvent(event);


        return providerAvailabilityMapper.toResponse(updated);
    }

    public List<ProviderAvailabilityResponseDTO> getAllProvider(){
        List<ProviderAvailability> resultAllProvidersAvailable = providerRepository.findAll();

        if(resultAllProvidersAvailable.isEmpty()){
            throw new ApiException("Provider availability not found", HttpStatus.NOT_FOUND);
        }
        return resultAllProvidersAvailable.stream()
            .map(provider -> providerAvailabilityMapper.toResponse(provider))
            .collect(Collectors.toList());
    }


}
