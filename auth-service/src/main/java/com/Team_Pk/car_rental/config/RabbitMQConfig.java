package com.Team_Pk.car_rental.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "stilles_auto_exchange";

    // Les clés de routage (Routing Keys)
    public static final String USER_VERIFIED_KEY = "user.verified"; // Pour le mail de Bienvenue
    public static final String PASSWORD_CHANGED_KEY = "user.password_changed"; // Pour l'alerte de sécurité

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); // Convertit nos objets Java en JSON pour RabbitMQ
    }
}