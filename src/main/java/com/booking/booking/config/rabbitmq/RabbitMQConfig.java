package com.booking.booking.config.rabbitmq;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.*;

@Profile("!test")
@Configuration
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Autowired
    private RabbitMQProperties props;
   


    // ===========
    // buildqueue helper
    // ===========

    private Queue buildQueue(String queueName, String dlqName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "") // Exchange padrão para DLQ
                .withArgument("x-dead-letter-routing-key", dlqName) // Rota para a DLQ
                .build();
    }

    // ===============
    // Booking
    // ===============
    @Bean
    TopicExchange bookingExchange(){
        return new TopicExchange(props.getBooking().getExchange());
    }

    @Bean
    Queue bookingCreatedQueue(){
        return buildQueue(props.getBooking().getQueue().getCreated(), 
        props.getBooking().getDlq().getCreated());
    }

    @Bean
    Queue bookingUpdatedQueue(){
        return buildQueue(props.getBooking().getQueue().getUpdated(), 
        props.getBooking().getDlq().getUpdated());
    }

    @Bean
    Queue bookingDeletedQueue(){
        return buildQueue(props.getBooking().getQueue().getDeleted(),
        props.getBooking().getDlq().getDeleted());
    }

    // ----------------------------------------
    // User QUEUE
    // ----------------------------------------

    @Bean 
    TopicExchange userExchange(){
        return new TopicExchange(props.getUsers().getExchange());
    }

    @Bean
    Queue userCreatedQueue(){
        return buildQueue(props.getUsers().getQueue().getCreated(),
        props.getUsers().getDlq().getCreated());
    }

    @Bean
    Queue userUpdatedQueue(){
        return buildQueue(props.getUsers().getQueue().getUpdated(),
        props.getUsers().getDlq().getUpdated());
    }

    @Bean
    Queue userDeletedQueue(){
        return buildQueue(props.getUsers().getQueue().getDeleted(),
        props.getUsers().getDlq().getDeleted());
    }

    // ----------------------------------------
    // PROVIDER QUEUE
    // ----------------------------------------

    @Bean
    TopicExchange providerExchange(){
        return new TopicExchange(props.getProvider().getExchange());
    }

    @Bean
    Queue providerCreatedQueue(){   
        return buildQueue(props.getProvider().getQueue().getCreated(),
        props.getProvider().getDlq().getCreated());
    }

    @Bean
    Queue providerUpdatedQueue(){
        return buildQueue(props.getProvider().getQueue().getUpdated(),
        props.getProvider().getDlq().getUpdated());
    }

    @Bean
    Queue providerDeletedQueue(){
        return buildQueue(props.getProvider().getQueue().getDeleted(),
        props.getProvider().getDlq().getDeleted());
    }


    // ----------------------------------------
    // DLQs
    // ----------------------------------------

    @Bean
    Queue bookingCreatedDlq(){
        return QueueBuilder.durable(props.getBooking().getDlq().getCreated()).build();
    }

    @Bean
    Queue bookingUpdatedDlq(){
        return QueueBuilder.durable(props.getBooking().getDlq().getUpdated()).build();
    }

    @Bean
    Queue bookingDeletedDlq(){
        return QueueBuilder.durable(props.getBooking().getDlq().getDeleted()).build();
    }   


    @Bean
    Queue userCreatedDlq(){
        return QueueBuilder.durable(props.getUsers().getDlq().getCreated()).build();
    }

    @Bean
    Queue userUpdatedDlq(){
        return QueueBuilder.durable(props.getUsers().getDlq().getUpdated()).build();
    }   

    @Bean
    Queue userDeletedDlq(){
        return QueueBuilder.durable(props.getUsers().getDlq().getDeleted()).build();
    }   

    @Bean
    Queue providerCreatedDlq(){  
        return QueueBuilder.durable(props.getProvider().getDlq().getCreated()).build();
    }   

    @Bean
    Queue providerUpdatedDlq(){
        return QueueBuilder.durable(props.getProvider().getDlq().getUpdated()).build();
    }

    @Bean
    Queue providerDeletedDlq(){
        return QueueBuilder.durable(props.getProvider().getDlq().getDeleted()).build();
    }

     // ==============================
    // Bindings
    // ==============================
    // Booking
    @Bean
    Binding bookingCreatedBinding(
            @Qualifier("bookingCreatedQueue") Queue bookingCreatedQueue,
            @Qualifier("bookingExchange") TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingCreatedQueue)
                .to(bookingExchange)
                .with(props.getBooking().getRouting().getCreated());
    }

    @Bean
    Binding bookingUpdatedBinding(
            @Qualifier("bookingUpdatedQueue") Queue bookingUpdatedQueue,
            @Qualifier("bookingExchange") TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingUpdatedQueue)
                .to(bookingExchange)
                .with(props.getBooking().getRouting().getUpdated());
    }

    @Bean
    Binding bookingDeletedBinding(
            @Qualifier("bookingDeletedQueue") Queue bookingDeletedQueue,
            @Qualifier("bookingExchange") TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingDeletedQueue)
                .to(bookingExchange)
                .with(props.getBooking().getRouting().getDeleted());
    }

    // User
    @Bean
    Binding userCreatedBinding(
            @Qualifier("userCreatedQueue") Queue userCreatedQueue,
            @Qualifier("userExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(userExchange)
                .with(props.getUsers().getRouting().getCreated());
    }

    @Bean
    Binding userUpdatedBinding(
            @Qualifier("userUpdatedQueue") Queue userUpdatedQueue,
            @Qualifier("userExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(userUpdatedQueue)
                .to(userExchange)
                .with(props.getUsers().getRouting().getUpdated());
    }

    @Bean
    Binding userDeletedBinding(
            @Qualifier("userDeletedQueue") Queue userDeletedQueue,
            @Qualifier("userExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueue)
                .to(userExchange)
                .with(props.getUsers().getRouting().getDeleted());
    }

    // Provider
    @Bean
    Binding providerCreatedBinding(
            @Qualifier("providerCreatedQueue") Queue providerCreatedQueue,
            @Qualifier("providerExchange") TopicExchange providerExchange) {
        return BindingBuilder.bind(providerCreatedQueue)
                .to(providerExchange)
                .with(props.getProvider().getRouting().getCreated());
    }

    @Bean
    Binding providerUpdatedBinding(
            @Qualifier("providerUpdatedQueue") Queue providerUpdatedQueue,
            @Qualifier("providerExchange") TopicExchange providerExchange) {
        return BindingBuilder.bind(providerUpdatedQueue)
                .to(providerExchange)
                .with(props.getProvider().getRouting().getUpdated());
    }

    @Bean
    Binding providerDeletedBinding(
            @Qualifier("providerDeletedQueue") Queue providerDeletedQueue,
            @Qualifier("providerExchange") TopicExchange providerExchange) {
        return BindingBuilder.bind(providerDeletedQueue)
                .to(providerExchange)
                .with(props.getProvider().getRouting().getDeleted());
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
