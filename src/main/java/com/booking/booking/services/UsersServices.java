package com.booking.booking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.UserRequestDTO;
import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.events.usersEvents.UsersDeletedEvent;
import com.booking.booking.events.usersEvents.UsersUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

@Service
public class UsersServices {

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private MessageProducerUsers messageProducerUsers;

    public UserResponseDTO saveUser(UserRequestDTO requestDTO){
        usersRepository.findByEmail(requestDTO.email()).ifPresent(u ->{
            throw new ApiException("Email already in use",HttpStatus.CONFLICT);
        });
        Users user = Users.builder()
            .name(requestDTO.name())
            .email(requestDTO.email())
            .roles(requestDTO.roles())  
            .createdAt(LocalDateTime.now())
            .build();
        
        Users userSaved = usersRepository.save(user);

        UsersCreatedEvent usersCreatedEvent = UsersCreatedEvent.builder()
            .id(userSaved.getId())
            .email(userSaved.getEmail())
            .name(userSaved.getName())
            .roles(userSaved.getRoles().toString())
            .eventTs(LocalDateTime.now())
            .build();

        // send to rabbitmq
        messageProducerUsers.sendUsersCreateEvent(usersCreatedEvent);

        return new UserResponseDTO(
            userSaved.getId(),
            userSaved.getName(),
            userSaved.getEmail(),
            userSaved.getRoles(),
            userSaved.getCreatedAt()
        );
    }

    public UserResponseDTO deleteUser(Long id){
        if(usersRepository.findById(id).isEmpty()){
            throw new ApiException("USER NOT FOUND", HttpStatus.NOT_FOUND);
        }

        Optional<Users> userFound = usersRepository.findById(id);
        usersRepository.deleteById(id);

        UsersDeletedEvent usersDeletedEvent = UsersDeletedEvent.builder()
            .id(userFound.get().getId())
            .name(userFound.get().getName())
            .email(userFound.get().getEmail())
            .roles(userFound.get().getRoles().toString())
            .createdAt(userFound.get().getCreatedAt())
            .build();

        // send to rabbitmq    
        messageProducerUsers.sendProviderDeleteEvent(usersDeletedEvent);

        return new UserResponseDTO(
        userFound.get().getId(),
        userFound.get().getName(),
        userFound.get().getEmail(),
        userFound.get().getRoles(),
        userFound.get().getCreatedAt());
    }

    public UserResponseDTO updateUser(Long id,UserRequestDTO userRequestDTO){
        Users userFound = usersRepository.findById(id).orElseThrow(() ->{
            throw new ApiException("USER NOT FOUND",HttpStatus.NOT_FOUND);
        });

        userFound.setName(userRequestDTO.name());
        userFound.setEmail(userRequestDTO.email());
        userFound.setRoles(userRequestDTO.roles());
        
        Users userUpdated = usersRepository.save(userFound);

        UsersUpdatedEvent usersUpdatedEvent = UsersUpdatedEvent.builder()
            .id(userUpdated.getId())
            .name(userUpdated.getName())
            .email(userUpdated.getEmail())
            .roles(userUpdated.getRoles().toString())
            .createdAt(LocalDateTime.now())
            .build();
        
        messageProducerUsers.sendProviderUpdateEvent(usersUpdatedEvent);

        return new UserResponseDTO(
            userUpdated.getId(),
            userUpdated.getName(),
            userUpdated.getEmail(),
            userUpdated.getRoles(),
            userUpdated.getCreatedAt()
        );
    }

    public UserResponseDTO getUserById(Long id){
        if(usersRepository.findById(id).isEmpty()){
            throw new ApiException("USER NOT FOUND", HttpStatus.NOT_FOUND);
        }

        Optional<Users> userFound = usersRepository.findById(id);
        return new UserResponseDTO(
            userFound.get().getId(),
            userFound.get().getName(),
            userFound.get().getEmail(),
            userFound.get().getRoles(),
            userFound.get().getCreatedAt()
        );
    }

    public List<UserResponseDTO> getAllUsers(){
        List<Users> usersFound = usersRepository.findAll();

        if(usersFound.isEmpty()){
            throw new ApiException("USER NOT FOUND", HttpStatus.NOT_FOUND);
        }

        return usersFound.stream()
            .map(user -> new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt()
            )).collect(Collectors.toList());
    }

}
