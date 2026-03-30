package com.booking.booking.services.rabbitMQEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.booking.booking.DTO.EventDTO;
import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityDeletedEvent;
import com.booking.booking.events.providerEvents.ProviderAvailabilityUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageProducerProvider {
    private static final Logger log = LoggerFactory.getLogger(MessageProducerProvider.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.provider.exchange}")
    private String providerExchange;

    @Value("${rabbitmq.provider.routing.created}")
    private String createdRK;

    @Value("${rabbitmq.provider.routing.updated}")
    private String updatedRK;

    @Value("${rabbitmq.provider.routing.deleted}")
    private String deletedRK;

    public void sendEvent(EventDTO<?> event) {

        String routingKey = resolveRoutingKey(event.type().toString());

        rabbitTemplate.convertAndSend(providerExchange, routingKey, event);

        log.info("📤 Evento enviado | type={} routingKey={} recipients={}",
                event.type(), routingKey, event.recipients());
    }

    private String resolveRoutingKey(String type) {
        return switch (type) {
            case "PROVIDER_CREATED" -> createdRK;
            case "PROVIDER_UPDATED" -> updatedRK;
            case "PROVIDER_DELETED" -> deletedRK;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + type);
        };
    }
}
