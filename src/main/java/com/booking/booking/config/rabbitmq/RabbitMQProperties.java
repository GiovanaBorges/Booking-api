package com.booking.booking.config.rabbitmq;

import java.security.Provider.Service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
    private ServiceConfig booking;
    private ServiceConfig provider;
    private ServiceConfig users;

     public ServiceConfig getBooking() {
        return booking;
    }

    public void setBooking(ServiceConfig booking) {
        this.booking = booking;
    }

    public ServiceConfig getProvider() {
        return provider;
    }

    public void setProvider(ServiceConfig provider) {
        this.provider = provider;
    }

    public ServiceConfig getUsers() {
        return users;
    }

    public void setUsers(ServiceConfig users) {
        this.users = users;
    }
}
