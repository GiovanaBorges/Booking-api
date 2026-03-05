package com.booking.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.booking.booking.config.rabbitmq.RabbitMQProperties;

@EnableConfigurationProperties(RabbitMQProperties.class)
@SpringBootApplication
@EnableJpaAuditing
public class BookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingApplication.class, args);
	}

}
