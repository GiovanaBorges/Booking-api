package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.booking.booking.events.usersEvents.UsersCreatedEvent;
import com.booking.booking.events.usersEvents.UsersDeletedEvent;
import com.booking.booking.events.usersEvents.UsersUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageProducerUsers {
    private static final Logger log = LoggerFactory.getLogger(MessageProducerUsers.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.users.exchange}")
    private String usersExchange;

    @Value("${rabbitmq.users.routing.created}")
    private String createdRK;

    @Value("${rabbitmq.users.routing.updated}")
    private String updatedRK;

    @Value("${rabbitmq.users.routing.deleted}")
    private String deletedRK;

    public void sendUsersCreateEvent(UsersCreatedEvent event){
        rabbitTemplate.convertAndSend(usersExchange,createdRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendProviderUpdateEvent(UsersUpdatedEvent event){
        rabbitTemplate.convertAndSend(usersExchange,updatedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendProviderDeleteEvent(UsersDeletedEvent event){
        rabbitTemplate.convertAndSend(usersExchange,deletedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }
}
