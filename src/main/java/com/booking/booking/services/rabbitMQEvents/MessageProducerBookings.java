package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public void sendBookingCreateEvent(BookingCreatedEvent event){
        rabbitTemplate.convertAndSend(bookingsExchange,createdRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendBookingUpdateEvent(BookingUpdatedEvent event){
        rabbitTemplate.convertAndSend(bookingsExchange,updatedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendBookingDeleteEvent(BookingDeletedEvent event){
        rabbitTemplate.convertAndSend(bookingsExchange,deletedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }
}
