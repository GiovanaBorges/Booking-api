package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageProducerProvider {
    private static final Logger log = LoggerFactory.getLogger(MessageProducerProvider.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbit.provider.exchange}")
    private String providerExchange;

    @Value("${rabbit.provider.exchange}")
    private String exchange;

    @Value("${rabbit.provider.routing.created}")
    private String createdRK;

    @Value("${rabbit.provider.routing.updated}")
    private String updatedRK;

    @Value("${rabbit.provider.routing.deleted}")
    private String deletedRK;

    public void sendProviderCreateEvent(ProviderAvailabilityCreatedEvent event){
        rabbitTemplate.convertAndSend(providerExchange,createdRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendProviderUpdateEvent(ProviderAvailabilityUpdatedEvent event){
        rabbitTemplate.convertAndSend(providerExchange,updatedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }

    public void sendProviderDeleteEvent(ProviderAvailabilityDeletedEvent event){
        rabbitTemplate.convertAndSend(providerExchange,deletedRK,event);
        log.info("📤 [PRODUCER] Evento enviado para RabbitMQ: {}", event);
    }
}
