package com.booking.booking.config.rabbitmq;

import lombok.Data;

@Data
public class ServiceConfig {
    private String exchange;
    private Routing routing;
    private QueueConfig queue;
    private DlqConfig dlq;
}
