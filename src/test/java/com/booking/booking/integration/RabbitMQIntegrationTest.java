package com.booking.booking.integration;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.booking.booking.events.providerEvents.ProviderAvailabilityCreatedEvent;
import com.booking.booking.services.rabbitMQEvents.MessageProducerProvider;

@SpringBootTest
@Testcontainers
@RabbitListenerTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = RabbitMQIntegrationTest.Initializer.class)
public class RabbitMQIntegrationTest {
     // ----------- SUBINDO O RABBITMQ COM TESTCONTAINERS ------------
    @Container
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageProducerProvider producer;

    // fila onde vamos capturar as mensagens recebidas
    private static BlockingQueue<ProviderAvailabilityCreatedEvent> queue = new LinkedBlockingQueue<>();


    // ------------ CONSUMIDOR FAKE PARA TESTE ------------
    @RabbitListener(queues = "${rabbitmq.provider.queue.created}")
    public void listen(ProviderAvailabilityCreatedEvent event) {
        queue.add(event);
    }


    // -------------- INICIALIZADOR DINÂMICO -------------
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.rabbitmq.host=" + rabbitmq.getHost(),
                "spring.rabbitmq.port=" + rabbitmq.getAmqpPort(),
                "spring.rabbitmq.username=guest",
                "spring.rabbitmq.password=guest"
            ).applyTo(context.getEnvironment());
        }
    }


    // ======================================================
    //                     TESTE PRINCIPAL
    // ======================================================
    @Test
    void testSendAndReceiveProviderCreatedEvent() throws Exception {

        ProviderAvailabilityCreatedEvent event = ProviderAvailabilityCreatedEvent.builder()
                .id(99L)
                .day_of_week(6)
                .start_time(LocalDateTime.now().toLocalTime())
                .end_time(LocalDateTime.now().toLocalTime())
                .providerId(10L)
                .eventTs(LocalDateTime.now())
                .build();

        // produz evento para exchange real
        producer.sendProviderCreateEvent(event);

        // aguarda receber na fila do listener
        ProviderAvailabilityCreatedEvent received =
                queue.take(); // bloqueia até receber

        // ---------- Asserts ----------
        Assertions.assertNotNull(received);
        Assertions.assertEquals(event.getId(), received.getId());
        Assertions.assertEquals(event.getProviderId(), received.getProviderId());
        Assertions.assertEquals(event.getDay_of_week(), received.getDay_of_week());
    }
}
