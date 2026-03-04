package com.Team_Pk.car_rental.community.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "stilles_auto_exchange";

    public static final String REVIEW_SUBMITTED_KEY = "review.submitted";
    public static final String REVIEW_APPROVED_KEY = "review.approved";
    public static final String REVIEW_REJECTED_KEY = "review.rejected";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}