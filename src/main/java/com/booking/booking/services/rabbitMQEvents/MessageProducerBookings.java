package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.EventDTO;
import com.booking.booking.events.bookingEvents.BookingCreatedEvent;
import com.booking.booking.events.bookingEvents.BookingDeletedEvent;
import com.booking.booking.events.bookingEvents.BookingUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageProducerBookings {
    private static final Logger log = LoggerFactory.getLogger(MessageProducerBookings.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.booking.exchange}")
    private String bookingsExchange;

    @Value("${rabbitmq.booking.routing.created}")
    private String createdRK;

    @Value("${rabbitmq.booking.routing.updated}")
    private String updatedRK;

    @Value("${rabbitmq.booking.routing.deleted}")
    private String deletedRK;

   public void sendEvent(EventDTO<?> event) {

        String routingKey = resolveRoutingKey(event.type().toString());

        rabbitTemplate.convertAndSend(bookingsExchange, routingKey, event);

        log.info("📤 Evento enviado | type={} routingKey={} recipients={}",
                event.type(), routingKey, event.recipients());
    }

    private String resolveRoutingKey(String type) {
        return switch (type) {
            case "BOOKING_CREATED" -> createdRK;
            case "BOOKING_UPDATED" -> updatedRK;
            case "BOOKING_DELETED" -> deletedRK;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + type);
        };
    }
}
