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
    @Value("${rabbitmq.booking.routing.created}") private String bookingRoutingKeyCreated;
    @Value("${rabbitmq.booking.routing.updated}") private String bookingRoutingKeyUpdated;
    @Value("${rabbitmq.booking.routing.deleted}") private String bookingRoutingKeyDeleted;
    @Value("${rabbitmq.booking.queue.created}")   private String bookingCreatedQueue;
    @Value("${rabbitmq.booking.queue.updated}")   private String bookingUpdatedQueue;
    @Value("${rabbitmq.booking.queue.deleted}")   private String bookingDeletedQueue;
    @Value("${rabbitmq.booking.dlq.created}")     private String bookingDlqCreated;
    @Value("${rabbitmq.booking.dlq.updated}")     private String bookingDlqUpdated;
    @Value("${rabbitmq.booking.dlq.deleted}")     private String bookingDlqDeleted;

    // PROVIDER
    @Value("${rabbitmq.provider.exchange}")       private String providerExchange;
    @Value("${rabbitmq.provider.routing.created}")private String providerRoutingKeyCreated;
    @Value("${rabbitmq.provider.routing.updated}")private String providerRoutingKeyUpdated;
    @Value("${rabbitmq.provider.routing.deleted}")private String providerRoutingKeyDeleted;
    @Value("${rabbitmq.provider.queue.created}")  private String providerCreatedQueue;
    @Value("${rabbitmq.provider.queue.updated}")  private String providerUpdatedQueue;
    @Value("${rabbitmq.provider.queue.deleted}")  private String providerDeletedQueue;
    @Value("${rabbitmq.provider.dlq.created}")    private String providerDlqCreate;
    @Value("${rabbitmq.provider.dlq.updated}")    private String providerDlqUpdated;
    @Value("${rabbitmq.provider.dlq.deleted}")    private String providerDlqDeleted;

    // USERS
    @Value("${rabbitmq.users.exchange}")          private String userExchange;
    @Value("${rabbitmq.users.routing.created}")   private String userRoutingKeyCreated;
    @Value("${rabbitmq.users.routing.updated}")   private String userRoutingKeyUpdated;
    @Value("${rabbitmq.users.routing.deleted}")   private String userRoutingKeyDeleted;
    @Value("${rabbitmq.users.queue.created}")     private String userCreatedQueue;
    @Value("${rabbitmq.users.queue.updated}")     private String userUpdatedQueue;
    @Value("${rabbitmq.users.queue.deleted}")     private String userDeletedQueue;
    @Value("${rabbitmq.users.dlq.created}")       private String userDlqCreate;
    @Value("${rabbitmq.users.dlq.updated}")       private String userDlqUpdated;
    @Value("${rabbitmq.users.dlq.deleted}")       private String userDlqDeleted;

    // ----------------------------------------
    // EXCHANGES
    // ----------------------------------------

     @Bean
    public TopicExchange bookingTopic() {
        return new TopicExchange(bookingExchange);
    }

    @Bean
    public TopicExchange providerTopic() {
        return new TopicExchange(providerExchange);
    }

    @Bean
    public TopicExchange userTopic() {
        return new TopicExchange(userExchange);
    }


    // ----------------------------------------
    // QUEUES + DLQs
    // ----------------------------------------

      private Queue buildQueue(String queue, String dlq) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", dlq)
                .build();
    }

    @Bean public Queue bookingCreatedQueue() { return buildQueue(bookingCreatedQueue, bookingDlqCreated); }
    @Bean public Queue bookingUpdatedQueue() { return buildQueue(bookingUpdatedQueue, bookingDlqUpdated); }
    @Bean public Queue bookingDeletedQueue() { return buildQueue(bookingDeletedQueue, bookingDlqDeleted); }

    // ----------------------------------------
    // DQL BOOKING
    // ----------------------------------------

    @Bean public Queue bookingDlqQueueCreated()     { return QueueBuilder.durable(bookingDlqCreated).build(); }
    @Bean public Queue bookingDlqQueueUpdated()     { return QueueBuilder.durable(bookingDlqUpdated).build(); }
    @Bean public Queue bookingDlqQueueDeleted()     { return QueueBuilder.durable(bookingDlqDeleted).build(); }

    @Bean public Queue providerCreatedQueue() { return buildQueue(providerCreatedQueue, providerDlqCreate); }
    @Bean public Queue providerUpdatedQueue() { return buildQueue(providerUpdatedQueue, providerDlqUpdated); }
    @Bean public Queue providerDeletedQueue() { return buildQueue(providerDeletedQueue, providerDlqDeleted); }

    // ----------------------------------------
    // DQL PROVIDER
    // ----------------------------------------

    @Bean public Queue providerDlqQueueCreated()     { return QueueBuilder.durable(providerDlqCreate).build(); }
    @Bean public Queue providerDlqQueueUpdated()     { return QueueBuilder.durable(providerDlqUpdated).build(); }
    @Bean public Queue providerDlqQueueDeleted()     { return QueueBuilder.durable(providerDlqDeleted).build(); }

    @Bean public Queue userCreatedQueue() { return buildQueue(userCreatedQueue, userDlqCreate); }
    @Bean public Queue userUpdatedQueue() { return buildQueue(userUpdatedQueue, userDlqUpdated); }
    @Bean public Queue userDeletedQueue() { return buildQueue(userDeletedQueue, userDlqDeleted); }

    // ----------------------------------------
    // DQL USERS
    // ----------------------------------------


    @Bean public Queue userDlqQueueCreated()     { return QueueBuilder.durable(userDlqCreate).build(); }
    @Bean public Queue userDlqQueueUpdated()     { return QueueBuilder.durable(userDlqUpdated).build(); }
    @Bean public Queue userDlqQueueDeleted()     { return QueueBuilder.durable(userDlqDeleted).build(); }


    // ----------------------------------------
    // BINDINGS
    // ----------------------------------------

    @Bean
    public Binding bookingBindingCreate() {
        return BindingBuilder.bind(bookingCreatedQueue())
                .to(bookingTopic()).with(bookingRoutingKeyCreated);
    }

    @Bean
    public Binding bookingBindingUpdate() {
        return BindingBuilder.bind(bookingUpdatedQueue())
                .to(bookingTopic()).with(bookingRoutingKeyUpdated);
    }

    @Bean
    public Binding bookingBindingDelete() {
        return BindingBuilder.bind(bookingDeletedQueue())
                .to(bookingTopic()).with(bookingRoutingKeyDeleted);
    }

    @Bean
    public Binding providerBindingCreate() {
        return BindingBuilder.bind(providerCreatedQueue())
                .to(providerTopic()).with(providerRoutingKeyCreated);
    }

    @Bean
    public Binding providerBindingUpdate() {
        return BindingBuilder.bind(providerUpdatedQueue())
                .to(providerTopic()).with(providerRoutingKeyUpdated);
    }

    @Bean
    public Binding providerBindingDelete() {
        return BindingBuilder.bind(providerDeletedQueue())
                .to(providerTopic()).with(providerRoutingKeyDeleted);
    }

    @Bean
    public Binding userBindingCreate() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userTopic()).with(userRoutingKeyCreated);
    }

    @Bean
    public Binding userBindingUpdate() {
        return BindingBuilder.bind(userUpdatedQueue())
                .to(userTopic()).with(userRoutingKeyUpdated);
    }

    @Bean
    public Binding userBindingDelete() {
        return BindingBuilder.bind(userDeletedQueue())
                .to(userTopic()).with(userRoutingKeyDeleted);
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
        return factory;
    }
}
