package com.booking.booking.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.booking.booking.DTO.UserRequestDTO;
import com.booking.booking.DTO.UserResponseDTO;
import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.events.usersEvents.UsersDeletedEvent;
import com.booking.booking.events.usersEvents.UsersUpdatedEvent;
import com.booking.booking.exceptions.ApiException;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.UsersRepository;
import com.booking.booking.services.rabbitMQEvents.MessageProducerUsers;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    @Mock
    private UsersRepository repository;

    @Mock
    private MessageProducerUsers messageProducerUsers;

    @InjectMocks
    private UsersServices service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateUser(){
        Users user = Users.builder()
            .id(1L)
            .email("email@email.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();
            
        when(repository.save(any(Users.class))).thenAnswer(i ->{
            Users u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponseDTO responseDTO = service.saveUser(
            new UserRequestDTO(
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles()
            )
        );

        assertAll(
            () -> assertEquals(user.getId(), responseDTO.id()),
            () -> assertEquals(user.getEmail(), responseDTO.email()),
            () -> assertEquals(user.getName(), responseDTO.name()),
            () -> assertEquals(user.getPassword(), responseDTO.password()),
            () -> assertEquals(user.getRoles(), responseDTO.roles())
        );

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerUsers, times(1))
            .sendUsersCreateEvent(any(UsersCreatedEvent.class));
    }

    @Test
    void shouldFindUserById(){
        Users user = Users.builder()
            .id(1L)
            .email("email@email.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

    
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponseDTO responseDTO = service.getUserById(1L);

        assertAll(
            () -> assertEquals(user.getId(), responseDTO.id()),
            () -> assertEquals(user.getEmail(), responseDTO.email()),
            () -> assertEquals(user.getName(), responseDTO.name()),
            () -> assertEquals(user.getPassword(), responseDTO.password()),
            () -> assertEquals(user.getRoles(), responseDTO.roles())
        );
    }

    @Test
    void shouldReturnErrorOnGetUserById(){
        when(repository.findById(1L)).thenReturn(Optional.empty());
        
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.getUserById(1L);
        });
        assertAll(
            () -> assertEquals("USER NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }

    @Test
    void shouldDeleteUser(){
         Users user = Users.builder()
            .id(1L)
            .email("email@email.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .createdAt(LocalDateTime.now())
            .build();

        
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        doNothing().when(repository).deleteById(1L);

        UserResponseDTO responseDTO = service.deleteUser(user.getId());

        assertAll(
            () -> assertEquals(user.getId(), responseDTO.id()),
            () -> assertEquals(user.getEmail(), responseDTO.email()),
            () -> assertEquals(user.getName(), responseDTO.name()),
            () -> assertEquals(user.getPassword(), responseDTO.password()),
            () -> assertEquals(user.getRoles(), responseDTO.roles())
        );

        // ================
        // RabbitMQ VERIFY
        // ================
        verify(messageProducerUsers, times(1))
            .sendProviderDeleteEvent(any(UsersDeletedEvent.class));
    }
    @Test
    void shoulReturnErrorOnDeleteUser(){
        when(repository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.deleteUser(1L);
        });
        assertAll(
            () -> assertEquals("USER NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }

    @Test
    void shouldUpdateUseruccessfully() {

        Users user = Users.builder()
                .id(10L)
                .email("provider@test.com")
                .name("Provider")
                .password("1234")
                .roles(RolesENUM.PROVIDER)
                .createdAt(LocalDateTime.now())
                .build();


         UserRequestDTO requestDTO = new UserRequestDTO(
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles()
            );

        when(repository.findById(10L)).thenReturn(Optional.of(user));
        when(repository.save(any(Users.class))).thenAnswer(inv -> inv.getArgument(0));

        // EXECUTE
        UserResponseDTO responseDTO = service.updateUser(10L, requestDTO);

        // VERIFY
        assertAll(
            () -> assertEquals(user.getId(), responseDTO.id()),
            () -> assertEquals(user.getEmail(), responseDTO.email()),
            () -> assertEquals(user.getName(), responseDTO.name()),
            () -> assertEquals(user.getPassword(), responseDTO.password()),
            () -> assertEquals(user.getRoles(), responseDTO.roles())
        );

        // repository interactions
        verify(repository, times(1)).findById(10L);
        verify(repository, times(1)).save(user);

        // Evento enviado ao RabbitMQ
         verify(messageProducerUsers, times(1))
            .sendProviderUpdateEvent(any(UsersUpdatedEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        UserRequestDTO request = new UserRequestDTO(
                "user",
                "email@email",
                "1234",
                RolesENUM.CLIENT
        );

        Assertions.assertThrows(ApiException.class, () -> {
            service.updateUser(99L, request);
        });

        verify(repository, times(1)).findById(99L);
        verify(messageProducerUsers, never()).sendProviderUpdateEvent(any());
    }

    @Test
    void shouldGetAllUsers(){
         Users user = Users.builder()
            .id(1L)
            .email("email@email.com")
            .name("user1")
            .password("1234")
            .roles(RolesENUM.PROVIDER)
            .build();

        Users user1 = Users.builder()
                .id(10L)
                .email("provider@test.com")
                .name("Provider")
                .password("1234")
                .roles(RolesENUM.CLIENT)
                .createdAt(LocalDateTime.now())
                .build();


        when(repository.findAll()).thenReturn(List.of(user,user1));
        List<UserResponseDTO> responseDTO = service.getAllUsers();
         assertAll(
            () -> assertEquals(user.getId(), responseDTO.get(0).id()),
            () -> assertEquals(user.getEmail(), responseDTO.get(0).email()),
            () -> assertEquals(user.getName(), responseDTO.get(0).name()),
            () -> assertEquals(user.getPassword(), responseDTO.get(0).password()),
            () -> assertEquals(user.getRoles(), responseDTO.get(0).roles())
        );
    }

    @Test
    void shouldReturnErrorOnGetAllUser(){
        when(repository.findAll()).thenReturn(List.of());
        ApiException exception = assertThrows(ApiException.class, () ->{
            service.getAllUsers();
        });
         assertAll(
            () -> assertEquals("USER NOT FOUND", exception.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatus())
        );
    }
}
