package com.booking.booking.config;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.*;

@Configuration
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    // BOOKING
    @Value("${rabbitmq.booking.exchange}")        private String bookingExchange;
    @Value("${rabbitmq.booking.queue}")           private String bookingQueue;
    @Value("${rabbitmq.booking.routing-key}")     private String bookingRoutingKey;
    @Value("${rabbitmq.booking.dlq}")             private String bookingDlq;

    // PROVIDER
    @Value("${rabbitmq.provider.exchange}")       private String providerExchange;
    @Value("${rabbitmq.provider.queue}")          private String providerQueue;
    @Value("${rabbitmq.provider.routing-key}")    private String providerRoutingKey;
    @Value("${rabbitmq.provider.dlq}")            private String providerDlq;

    // USERS
    @Value("${rabbitmq.users.exchange}")           private String userExchange;
    @Value("${rabbitmq.users.queue}")              private String userQueue;
    @Value("${rabbitmq.users.routing-key}")        private String userRoutingKey;
    @Value("${rabbitmq.users.dlq}")                private String userDlq;

     // ----------------------------------------
    // EXCHANGES
    // ----------------------------------------

    @Bean
    public TopicExchange bookingExchange() { 
        log.info("Registrando exchange booking: {}", bookingExchange);
        return new TopicExchange(bookingExchange); 
    }

    @Bean
    public TopicExchange providerExchange() { 
        log.info("Registrando exchange provider: {}", providerExchange);
        return new TopicExchange(providerExchange); 
    }

    @Bean
    public TopicExchange userExchange() { 
        log.info("Registrando exchange user: {}", userExchange);
        return new TopicExchange(userExchange); 
    }

    // ----------------------------------------
    // QUEUES + DLQs
    // ----------------------------------------

    @Bean
    public Queue bookingQueue() {
        log.info("Registrando fila booking: {}", bookingQueue);
        return QueueBuilder.durable(bookingQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", bookingDlq)
                .build();
    }

    @Bean
    public Queue bookingDlq() {
        log.info("Registrando DLQ of booking: {}", bookingDlq);
        return QueueBuilder.durable(bookingDlq).build();
    }


    @Bean
    public Queue providerQueue() {
        log.info("Registrando fila provider: {}", providerQueue);
        return QueueBuilder.durable(providerQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", providerDlq)
                .build();
    }

    @Bean
    public Queue providerDlq() {
        log.info("Registrando DLQ of provider: {}", providerDlq);
        return QueueBuilder.durable(providerDlq).build();
    }


    @Bean
    public Queue userQueue() {
        log.info("Registrando fila of user: {}", userQueue);
        return QueueBuilder.durable(userQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", userDlq)
                .build();
    }

    @Bean
    public Queue userDlq() {
        log.info("Registrando DLQ of user: {}", userDlq);
        return QueueBuilder.durable(userDlq).build();
    }


       // ----------------------------------------
    // BINDINGS
    // ----------------------------------------

    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange bookingExchange) {
        log.info("Criando binding entre {} e {} com routingKey={}", bookingExchange, bookingQueue, bookingRoutingKey);
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(bookingRoutingKey);
    }

    @Bean
    public Binding providerBinding(Queue providerQueue, TopicExchange providerExchange) {
        log.info("Criando binding entre {} e {} com routingKey={}", providerExchange, providerQueue, providerRoutingKey);
        return BindingBuilder.bind(providerQueue).to(providerExchange).with(providerRoutingKey);
    }

    @Bean
    public Binding userBinding(Queue userQueue, TopicExchange userExchange) {
        log.info("Criando binding entre {} e {} com routingKey={}", userExchange, userQueue, userRoutingKey);
        return BindingBuilder.bind(userQueue).to(userExchange).with(userRoutingKey);
    }

    // ==============================
    // 🔹 Conversor JSON (Jackson)
    // ==============================
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        log.info("Registrando Jackson2JsonMessageConverter para mensagens RabbitMQ");
        return new Jackson2JsonMessageConverter();
    }

    // ==============================
    // 🔹 RabbitTemplate configurado com JSON
    // ==============================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        log.info("RabbitTemplate configurado com Jackson2JsonMessageConverter");
        return template;
    }

    // ==============================
    // 🔹 Listener Container Factory (com retry e DLQ)
    // ==============================
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);

        // Evita reencaminhar mensagens falhas infinitamente
        factory.setDefaultRequeueRejected(false);

        // Paralelismo
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);

        // Retry automático
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffOptions(2000, 2.0, 10000)
                        .recoverer((msg, cause) ->
                                log.error("[DLQ] Mensagem movida após falhas permanentes. Causa: {}", cause.getMessage()))
                        .build()
        );

        log.info("Listener configurado com retry automático e DLQ para as filas {}", userQueue,bookingQueue,providerQueue);
        return factory;
    }
}
