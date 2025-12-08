package com.booking.booking.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.ProviderAvailabilityRequestDTO;
import com.booking.booking.DTO.ProviderAvailabilityResponseDTO;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
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

    public ProviderAvailabilityResponseDTO saveProviderAvailability(ProviderAvailabilityRequestDTO requestDTO){
        
        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("Provider not found", HttpStatus.NOT_FOUND));

        ProviderAvailability providerToBeSaved = ProviderAvailability.builder()
            .day_of_week(requestDTO.day_of_week())
            .end_time(requestDTO.end_time())
            .provider(provider)
            .start_time(requestDTO.startTime())
            .build();
        
        ProviderAvailability result = providerRepository.save(providerToBeSaved);

        // create event for rabbitMQ
        ProviderAvailabilityCreatedEvent event = ProviderAvailabilityCreatedEvent.builder()            
            .id(result.getId())
            .day_of_week(result.getDay_of_week())
            .end_time(result.getEnd_time())
            .start_time(result.getStart_time())
            .providerId(result.getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();
            
        
            // send event to RabbitMQ
            messageProducerProvider.sendProviderCreateEvent(event);

            return new ProviderAvailabilityResponseDTO(
                result.getId(),
                result.getDay_of_week(),
                result.getStart_time(),
                result.getEnd_time(),
                result.getProvider()
            );
    }

    public ProviderAvailabilityResponseDTO deleteProviderById(Long id){
        ProviderAvailability providerFound = providerRepository.findById(id)
            .orElseThrow(() -> new ApiException("nenhum provider encontrado", HttpStatus.NOT_FOUND));

        providerRepository.deleteById(id);


        // delete event for rabbitMQ
        ProviderAvailabilityDeletedEvent event = ProviderAvailabilityDeletedEvent.builder()            
            .id(providerFound.getId())
            .day_of_week(providerFound.getDay_of_week())
            .end_time(providerFound.getEnd_time())
            .start_time(providerFound.getStart_time())
            .providerId(providerFound.getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();
            
            // send event to RabbitMQ
            messageProducerProvider.sendProviderDeleteEvent(event);

        return new ProviderAvailabilityResponseDTO(
            providerFound.getId(),
            providerFound.getDay_of_week(),
            providerFound.getStart_time(),
            providerFound.getEnd_time(),
            providerFound.getProvider()
        );
    }

    public ProviderAvailabilityResponseDTO findProviderById(Long id){
        Optional<ProviderAvailability> providerAvailable = providerRepository.findById(id);
        if(providerAvailable.isEmpty()){
            throw new ApiException("Esse horário não foi encontrado",HttpStatus.NOT_FOUND);
        }
        return new ProviderAvailabilityResponseDTO(
            providerAvailable.get().getId(),
            providerAvailable.get().getDay_of_week(),
            providerAvailable.get().getStart_time(),
            providerAvailable.get().getEnd_time(),
            providerAvailable.get().getProvider()
        );
    }

    public ProviderAvailabilityResponseDTO updateProvider(Long id,ProviderAvailabilityRequestDTO requestDTO){
        ProviderAvailability providerFound = providerRepository.findById(id)
            .orElseThrow(() -> new ApiException("Provider availability not found", HttpStatus.NOT_FOUND));

        Users provider = usersRepository.findById(requestDTO.providerId())
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        // updating data
        providerFound.setDay_of_week(requestDTO.day_of_week());
        providerFound.setEnd_time(requestDTO.end_time());
        providerFound.setProvider(provider);
        providerFound.setStart_time(requestDTO.startTime());

        ProviderAvailability updated = providerRepository.save(providerFound);

         // delete event for rabbitMQ
        ProviderAvailabilityUpdatedEvent event = ProviderAvailabilityUpdatedEvent.builder()            
            .id(updated.getId())
            .day_of_week(updated.getDay_of_week())
            .end_time(updated.getEnd_time())
            .start_time(updated.getStart_time())
            .providerId(updated.getProvider().getId())
            .eventTs(LocalDateTime.now())
            .build();
            
            // send event to RabbitMQ
            messageProducerProvider.sendProviderUpdateEvent(event);


        return new ProviderAvailabilityResponseDTO(
            updated.getId(),
            updated.getDay_of_week(),
            updated.getStart_time(),
            updated.getEnd_time(),
            updated.getProvider()
        );
    }

    public List<ProviderAvailabilityResponseDTO> getAllProvider(){
        List<ProviderAvailability> resultAllProvidersAvailable = providerRepository.findAll();

        if(resultAllProvidersAvailable.isEmpty()){
            throw new ApiException("Não há nenhum horário cadastrado", HttpStatus.NOT_FOUND);
        }
        return resultAllProvidersAvailable.stream()
            .map(provider -> new ProviderAvailabilityResponseDTO(
              provider.getId(),
              provider.getDay_of_week(),
              provider.getStart_time(),
              provider.getEnd_time(),
              provider.getProvider()))
              .collect(Collectors.toList());
    }


}
