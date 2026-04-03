package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.EventDTO;
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

    public void sendEvent(EventDTO<?> event) {

        String routingKey = resolveRoutingKey(event.type().toString());

        rabbitTemplate.convertAndSend(usersExchange, routingKey, event);

        log.info("📤 Evento enviado | type={} routingKey={} recipients={}",
                event.type(), routingKey, event.recipients());
    }

    private String resolveRoutingKey(String type) {
        return switch (type) {
            case "USER_CREATED" -> createdRK;
            case "USER_UPDATED" -> updatedRK;
            case "USER_DELETED" -> deletedRK;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + type);
        };
    }
}
